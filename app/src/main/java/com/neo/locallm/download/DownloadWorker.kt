package com.neo.locallm.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DownloadWorker"

        const val KEY_URL = "url"
        const val KEY_FILENAME = "filename"
        const val KEY_MODEL_NAME = "model_name"
        const val KEY_STORAGE_URI = "storage_uri"
        const val KEY_WORK_NAME = "work_name"

        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_SPEED_BYTES_PER_SEC = "speed_bytes_per_sec"
        const val KEY_ETA_SECONDS = "eta_seconds"
        const val KEY_STATUS = "status"
        const val KEY_PROGRESS = "progress"

        const val KEY_ERROR = "error"

        const val TAG_MODEL_DOWNLOAD = "model_download"

        private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
        private const val SPEED_WINDOW_MS = 3000L
        private const val BUFFER_SIZE = 1024 * 1024
        private const val SAF_COPY_BUFFER_SIZE = 1024 * 1024
        private const val PARALLEL_DOWNLOAD_SEGMENTS = 4
        private const val PARALLEL_MIN_BYTES = 64L * 1024L * 1024L
    }

    private val notificationManager = DownloadNotificationManager(applicationContext)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Override required for the framework to query our foreground info up-front
     * (e.g. when running as expedited work) without relying on `doWork()` having
     * reached its `setForeground()` call. Returning a fully-formed
     * [ForegroundInfo] here is the documented `CoroutineWorker` pattern for
     * avoiding `ForegroundServiceDidNotStartInTimeException`.
     */
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: ""
        val filename = inputData.getString(KEY_FILENAME) ?: ""
        val workName = inputData.getString(KEY_WORK_NAME) ?: "download_$filename"
        val notificationId = notificationManager.getNotificationId(modelName)
        val notification = notificationManager.buildProgressNotification(
            modelName = modelName,
            progress = 0f,
            bytesDownloaded = 0,
            totalBytes = 0,
            speedBytesPerSec = 0,
            etaSeconds = 0,
            workName = workName
        )
        return ForegroundInfo(
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    override suspend fun doWork(): Result {
        // Promote to foreground IMMEDIATELY, before any other work. The system
        // gives us ~5s after startForegroundService() to call setForeground; on
        // a cold-started low-end device the OkHttpClient construction and
        // inputData reads below can eat into that budget. Building the
        // ForegroundInfo in getForegroundInfo() also lets the framework promote
        // us itself if it queries first.
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.w(TAG, "Could not set foreground: ${e.message}")
        }

        val url = inputData.getString(KEY_URL) ?: return failure("Missing download URL")
        val filename = inputData.getString(KEY_FILENAME) ?: return failure("Missing filename")
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: return failure("Missing model name")
        val storageUriString = inputData.getString(KEY_STORAGE_URI) ?: return failure("Missing storage URI")
        val workName = inputData.getString(KEY_WORK_NAME) ?: "download_$filename"

        val tempFile = File(applicationContext.getExternalFilesDir(null), filename)
        val notificationId = notificationManager.getNotificationId(modelName)

        try {
            val downloadResult = downloadFile(url, tempFile, modelName, workName, notificationId)
            if (!downloadResult) {
                return failure("Download stopped")
            }

            reportStatus(modelName, -1f, "Moving to storageâ€¦", tempFile.length(), tempFile.length(), 0, 0)
            // Update the pinned FGS notification directly; we are already a
            // foreground service so notify() updates the existing notification
            // rather than re-promoting via SystemForegroundService.
            notificationManager.showNotification(
                notificationId,
                notificationManager.buildCopyingNotification(modelName)
            )

            val storageUri = Uri.parse(storageUriString)
            val copyResult = copyToSafStorage(tempFile, filename, storageUri)
            if (!copyResult) {
                tempFile.delete()
                return failure("Failed to copy to storage")
            }

            tempFile.delete()

            notificationManager.showNotification(
                notificationId,
                notificationManager.buildCompleteNotification(modelName)
            )

            return Result.success()
        } catch (e: IOException) {
            Log.e(TAG, "Download IO error: ${e.message}", e)
            notificationManager.showNotification(
                notificationId,
                notificationManager.buildFailureNotification(modelName, e.message ?: "Network error")
            )
            return Result.retry()
        } catch (e: Exception) {
            Log.e(TAG, "Download error: ${e.message}", e)
            tempFile.delete()
            notificationManager.showNotification(
                notificationId,
                notificationManager.buildFailureNotification(modelName, e.message ?: "Download failed")
            )
            return failure(e.message ?: "Download failed")
        }
    }

    private suspend fun downloadFile(
        url: String,
        tempFile: File,
        modelName: String,
        workName: String,
        notificationId: Int
    ): Boolean {
        var existingBytes = 0L
        var append = false

        if (tempFile.exists() && tempFile.length() > 0) {
            existingBytes = tempFile.length()
            append = true
            Log.d(TAG, "Resuming download from byte $existingBytes")
        }

        if (!append) {
            val probe = probeDownload(url)
            if (probe.supportsRanges && probe.contentLength >= PARALLEL_MIN_BYTES) {
                try {
                    return downloadFileParallel(
                        url = url,
                        tempFile = tempFile,
                        totalBytes = probe.contentLength,
                        modelName = modelName,
                        workName = workName,
                        notificationId = notificationId
                    )
                } catch (e: IOException) {
                    Log.w(TAG, "Parallel download failed, falling back to single stream: ${e.message}")
                    tempFile.delete()
                }
            }
        }

        val requestBuilder = Request.Builder().url(url)
        if (append) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (!response.isSuccessful && response.code != 206) {
            response.close()
            throw IOException("HTTP ${response.code}: ${response.message}")
        }

        if (append && response.code != 206) {
            Log.d(TAG, "Server does not support Range, restarting from scratch")
            existingBytes = 0L
            append = false
        }

        val body = response.body ?: throw IOException("Empty response body")

        val contentLength = body.contentLength()
        val totalBytes = if (contentLength > 0) {
            existingBytes + contentLength
        } else {
            -1L
        }

        val speedTracker = SpeedTracker()

        val outputStream = FileOutputStream(tempFile, append)
        val inputStream = body.byteStream()

        var bytesDownloaded = existingBytes
        var lastProgressUpdate = 0L

        try {
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                if (isStopped) {
                    Log.d(TAG, "Download cancelled")
                    return false
                }

                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break

                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                speedTracker.addBytes(bytesRead.toLong())

                val now = System.currentTimeMillis()
                if (now - lastProgressUpdate >= PROGRESS_UPDATE_INTERVAL_MS) {
                    lastProgressUpdate = now

                    val speed = speedTracker.getSpeedBytesPerSec()
                    val progress = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
                    val eta = if (totalBytes > 0 && speed > 0) {
                        (totalBytes - bytesDownloaded) / speed
                    } else {
                        -1L
                    }

                    reportStatus(modelName, progress, "Downloadingâ€¦", bytesDownloaded, totalBytes, speed, eta)

                    // Update the pinned FGS notification directly. Calling
                    // setForeground() every 500ms re-routes through
                    // SystemForegroundService and reopens the overlap window
                    // tracked in WorkManager bug b/432069314 (fixed in 2.10.5
                    // but the cheaper notify() path is the right pattern
                    // regardless). notify() updates the same notification ID
                    // that's pinned to our FGS, so the FGS state is preserved.
                    notificationManager.showNotification(
                        notificationId,
                        notificationManager.buildProgressNotification(
                            modelName, progress, bytesDownloaded, totalBytes, speed, eta, workName
                        )
                    )
                }
            }
            outputStream.flush()
        } finally {
            inputStream.close()
            outputStream.close()
            response.close()
        }

        return true
    }

    private fun probeDownload(url: String): DownloadProbe {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 206) {
                    val contentLength = response.header("Content-Range")?.let(::parseContentRangeLength) ?: -1L
                    return DownloadProbe(contentLength, contentLength > 0)
                }
                if (!response.isSuccessful) {
                    return DownloadProbe(-1L, false)
                }
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val supportsRanges = response.header("Accept-Ranges")
                    ?.contains("bytes", ignoreCase = true) == true
                DownloadProbe(contentLength, supportsRanges)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not probe download: ${e.message}")
            DownloadProbe(-1L, false)
        }
    }

    private fun parseContentRangeLength(value: String): Long {
        val slash = value.lastIndexOf('/')
        if (slash < 0 || slash == value.length - 1) return -1L
        return value.substring(slash + 1).trim().toLongOrNull() ?: -1L
    }

    private suspend fun downloadFileParallel(
        url: String,
        tempFile: File,
        totalBytes: Long,
        modelName: String,
        workName: String,
        notificationId: Int
    ): Boolean = coroutineScope {
        RandomAccessFile(tempFile, "rw").use { file ->
            file.setLength(totalBytes)
        }

        val segments = min(PARALLEL_DOWNLOAD_SEGMENTS, max(2, (totalBytes / PARALLEL_MIN_BYTES).toInt()))
        val partSize = (totalBytes + segments - 1) / segments
        val downloaded = AtomicLong(0L)
        val jobs = (0 until segments).map { part ->
            val start = part * partSize
            val end = min(totalBytes - 1, start + partSize - 1)
            async(Dispatchers.IO) {
                downloadRange(url, tempFile, start, end, downloaded)
            }
        }

        var lastBytes = 0L
        var lastUpdate = System.currentTimeMillis()
        while (jobs.any { !it.isCompleted }) {
            if (isStopped) {
                jobs.forEach { it.cancel() }
                return@coroutineScope false
            }

            delay(PROGRESS_UPDATE_INTERVAL_MS)
            val now = System.currentTimeMillis()
            val bytes = downloaded.get()
            val elapsedMs = now - lastUpdate
            val speed = if (elapsedMs > 0) ((bytes - lastBytes) * 1000L) / elapsedMs else 0L
            val progress = bytes.toFloat() / totalBytes
            val eta = if (speed > 0) (totalBytes - bytes) / speed else -1L
            reportStatus(modelName, progress, "Downloading with ${segments} connections...", bytes, totalBytes, speed, eta)
            notificationManager.showNotification(
                notificationId,
                notificationManager.buildProgressNotification(
                    modelName, progress, bytes, totalBytes, speed, eta, workName
                )
            )
            lastBytes = bytes
            lastUpdate = now
        }

        jobs.awaitAll()
        reportStatus(modelName, 1f, "Download complete", totalBytes, totalBytes, 0, 0)
        true
    }

    private fun downloadRange(
        url: String,
        tempFile: File,
        start: Long,
        end: Long,
        downloaded: AtomicLong
    ) {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=$start-$end")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code != 206) {
                throw IOException("Range request returned HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty range response")
            body.byteStream().use { input ->
                RandomAccessFile(tempFile, "rw").use { file ->
                    file.seek(start)
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        if (isStopped) throw IOException("Download stopped")
                        val read = input.read(buffer)
                        if (read == -1) break
                        file.write(buffer, 0, read)
                        downloaded.addAndGet(read.toLong())
                    }
                }
            }
        }
    }

    private suspend fun reportStatus(
        modelName: String,
        progress: Float,
        status: String,
        bytesDownloaded: Long,
        totalBytes: Long,
        speed: Long,
        eta: Long
    ) {
        setProgress(
            Data.Builder()
                .putString(KEY_MODEL_NAME, modelName)
                .putFloat(KEY_PROGRESS, progress)
                .putString(KEY_STATUS, status)
                .putLong(KEY_BYTES_DOWNLOADED, bytesDownloaded)
                .putLong(KEY_TOTAL_BYTES, totalBytes)
                .putLong(KEY_SPEED_BYTES_PER_SEC, speed)
                .putLong(KEY_ETA_SECONDS, eta)
                .build()
        )
    }

    private fun copyToSafStorage(tempFile: File, filename: String, storageUri: Uri): Boolean {
        val documentFile = DocumentFile.fromTreeUri(applicationContext, storageUri) ?: return false

        documentFile.findFile(filename)?.delete()

        val destFile = documentFile.createFile("application/octet-stream", filename) ?: return false

        applicationContext.contentResolver.openOutputStream(destFile.uri)?.use { outputStream ->
            tempFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream, bufferSize = SAF_COPY_BUFFER_SIZE)
            }
        } ?: return false

        return true
    }

    private data class DownloadProbe(val contentLength: Long, val supportsRanges: Boolean)

    private fun failure(message: String): Result {
        return Result.failure(
            Data.Builder()
                .putString(KEY_ERROR, message)
                .build()
        )
    }

    private class SpeedTracker {
        private data class Sample(val timestampMs: Long, val bytes: Long)

        private val samples = mutableListOf<Sample>()

        fun addBytes(bytes: Long) {
            samples.add(Sample(System.currentTimeMillis(), bytes))
            pruneOldSamples()
        }

        fun getSpeedBytesPerSec(): Long {
            pruneOldSamples()
            if (samples.size < 2) return 0

            val windowStart = samples.first().timestampMs
            val windowEnd = samples.last().timestampMs
            val durationMs = windowEnd - windowStart
            if (durationMs <= 0) return 0

            val totalBytes = samples.sumOf { it.bytes }
            return (totalBytes * 1000L) / durationMs
        }

        private fun pruneOldSamples() {
            val cutoff = System.currentTimeMillis() - SPEED_WINDOW_MS
            samples.removeAll { it.timestampMs < cutoff }
        }
    }
}
