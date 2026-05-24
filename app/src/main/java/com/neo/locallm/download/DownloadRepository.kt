package com.neo.locallm.download

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.neo.locallm.models.ModelInfo
import com.neo.locallm.storage.DownloadProgress
import java.io.File
import java.util.concurrent.TimeUnit

class DownloadRepository(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun startDownload(model: ModelInfo, storageUri: Uri) {
        val remoteUri = model.remoteUri ?: return
        enqueueDownload(
            modelName = model.name,
            filename = model.filename,
            remoteUri = remoteUri,
            storageUri = storageUri,
            workName = workNameFor(model.filename)
        )
        model.additionalFiles.forEach { part ->
            enqueueDownload(
                modelName = model.name,
                filename = part.filename,
                remoteUri = part.remoteUri,
                storageUri = storageUri,
                workName = workNameFor(part.filename)
            )
        }
    }

    private fun enqueueDownload(
        modelName: String,
        filename: String,
        remoteUri: Uri,
        storageUri: Uri,
        workName: String
    ) {
        val inputData = Data.Builder()
            .putString(DownloadWorker.KEY_URL, remoteUri.toString())
            .putString(DownloadWorker.KEY_FILENAME, filename)
            .putString(DownloadWorker.KEY_MODEL_NAME, modelName)
            .putString(DownloadWorker.KEY_STORAGE_URI, storageUri.toString())
            .putString(DownloadWorker.KEY_WORK_NAME, workName)
            .build()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(DownloadWorker.TAG_MODEL_DOWNLOAD)
            .addTag(modelName)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)
    }

    fun cancelDownload(model: ModelInfo) {
        workManager.cancelUniqueWork(workNameFor(model.filename))
        model.additionalFiles.forEach { part ->
            workManager.cancelUniqueWork(workNameFor(part.filename))
        }

        (listOf(model.filename) + model.additionalFiles.map { it.filename }).forEach { filename ->
            val tempFile = File(context.getExternalFilesDir(null), filename)
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }

        val notificationManager = DownloadNotificationManager(context)
        notificationManager.cancelNotification(notificationManager.getNotificationId(model.name))
    }

    fun observeDownloads(): LiveData<Map<String, DownloadProgress>> {
        return workManager
            .getWorkInfosByTagLiveData(DownloadWorker.TAG_MODEL_DOWNLOAD)
            .map { workInfoList -> mapWorkInfoToProgress(workInfoList) }
    }

    private fun mapWorkInfoToProgress(workInfoList: List<WorkInfo>): Map<String, DownloadProgress> {
        val result = mutableMapOf<String, DownloadProgress>()

        for (workInfo in workInfoList) {
            if (workInfo.state.isFinished) continue

            val progress = workInfo.progress
            val modelName = progress.getString(DownloadWorker.KEY_MODEL_NAME)

            if (modelName == null) {
                val tags = workInfo.tags
                val nameTag = tags.firstOrNull {
                    it != DownloadWorker.TAG_MODEL_DOWNLOAD && !it.startsWith("download_")
                }
                if (nameTag != null) {
                    val status = when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> "Waiting for networkâ€¦"
                        WorkInfo.State.BLOCKED -> "Waitingâ€¦"
                        else -> "Starting downloadâ€¦"
                    }
                    result[nameTag] = DownloadProgress(
                        modelName = nameTag,
                        progress = -1f,
                        status = status
                    )
                }
                continue
            }

            val progressValue = progress.getFloat(DownloadWorker.KEY_PROGRESS, 0f)
            val status = progress.getString(DownloadWorker.KEY_STATUS) ?: "Downloadingâ€¦"
            val bytesDownloaded = progress.getLong(DownloadWorker.KEY_BYTES_DOWNLOADED, 0L)
            val totalBytes = progress.getLong(DownloadWorker.KEY_TOTAL_BYTES, 0L)
            val speed = progress.getLong(DownloadWorker.KEY_SPEED_BYTES_PER_SEC, 0L)
            val eta = progress.getLong(DownloadWorker.KEY_ETA_SECONDS, -1L)

            result[modelName] = DownloadProgress(
                modelName = modelName,
                progress = progressValue,
                status = status,
                bytesDownloaded = bytesDownloaded,
                totalBytes = totalBytes,
                speedBytesPerSec = speed,
                etaSeconds = eta
            )
        }

        return result
    }

    private fun workNameFor(filename: String): String = "download_$filename"
}
