package com.neo.locallm.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.diazneoones82.llamacpp.InferenceState
import com.neo.locallm.App
import com.neo.locallm.R
import com.neo.locallm.models.ModelInfo
import com.neo.locallm.online.OnlinePreferences
import com.neo.locallm.storage.ModelsContent
import com.neo.locallm.storage.StorageViewModel
import com.neo.locallm.theme.PlaygroundTheme
import com.neo.locallm.theme.ThemePreferences
import com.neo.locallm.util.rememberHingeWidthDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    // Lazily created via Compose property delegate — only allocated on tablet
    // when the user actually opens a Models detail pane, since the
    // ViewModel hits storage as soon as it's constructed.
    private val storageViewModel: StorageViewModel by viewModels()

    // Pending download model held across the notification-permission
    // request, mirroring the original ModelsFragment behaviour.
    private var pendingDownloadModel: ModelInfo? = null

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            storageViewModel.requestStorageFolderChange(uri)
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        pendingDownloadModel?.let { model ->
            storageViewModel.downloadModel(model)
            pendingDownloadModel = null
        }
    }

    private fun startDownloadWithPermissionCheck(model: ModelInfo) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            storageViewModel.downloadModel(model)
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (granted || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS).not()
            && hasAskedNotificationPermission()
        ) {
            storageViewModel.downloadModel(model)
        } else {
            pendingDownloadModel = model
            setAskedNotificationPermission()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun hasAskedNotificationPermission(): Boolean {
        return requireContext().getSharedPreferences("download_prefs", 0)
            .getBoolean("asked_notification_permission", false)
    }

    private fun setAskedNotificationPermission() {
        requireContext().getSharedPreferences("download_prefs", 0)
            .edit().putBoolean("asked_notification_permission", true).apply()
    }

    /**
     * Rapid double-taps on a settings row can fire the second click after the
     * first has already navigated away from nav_settings, at which point the
     * action ID is no longer known to the NavController and navigate() throws
     * IllegalArgumentException. Guard every navigation by verifying the
     * NavController is still at nav_settings before firing the action.
     */
    private fun NavController.navigateFromSettings(actionId: Int) {
        if (currentDestination?.id == R.id.nav_settings) {
            navigate(actionId)
        }
    }

    /**
     * Debug-only: trigger a Process.killProcess(myPid) inside `:llama` so we
     * can verify that crashes don't take the UI down and that the
     * acknowledge → reload recovery path works.
     */
    private fun crashInferenceEngine() {
        val app = activity?.application as? App ?: return
        val client = app.inferenceClient
        val service = (client.state.value as? InferenceState.Connected)?.service
        if (service == null) {
            Toast.makeText(
                requireContext(),
                R.string.debug_crash_engine_not_connected,
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        // Run off the main thread — the binder call will throw DeadObject
        // because the process dies mid-transaction; that's expected.
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    service.crashForTest()
                } catch (t: Throwable) {
                    Log.d(TAG, "crashForTest returned with expected error: ${t.javaClass.simpleName}")
                }
            }
            Toast.makeText(
                requireContext(),
                R.string.debug_crash_engine_done,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    companion object {
        private const val TAG = "SettingsFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(inflater.context).apply {
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        setContent {
            PlaygroundTheme {
                val configuration = LocalConfiguration.current
                // Mirror the SettingsScreen.twoPane threshold so we only build
                // the inline detail content (and instantiate ViewModels) when
                // it'll actually render.
                val twoPane = configuration.screenWidthDp >= 840

                // Foldable book posture: line the master/detail boundary up
                // with the vertical hinge. Falls back to 360dp on flat
                // devices / horizontal hinges / folded outer display.
                val hingeWidth = rememberHingeWidthDp(requireActivity())
                // 320dp matches the chat sidebar default so both screens have
                // the same pane width on tablets without a hinge.
                val masterWidth = if (hingeWidth != androidx.compose.ui.unit.Dp.Unspecified) {
                    maxOf(320.dp, hingeWidth)
                } else {
                    320.dp
                }
                val onlinePreferences = remember { OnlinePreferences(requireContext()) }
                var huggingFaceToken by remember {
                    mutableStateOf(onlinePreferences.huggingFaceToken)
                }
                var openRouterApiKey by remember {
                    mutableStateOf(onlinePreferences.openRouterApiKey)
                }
                val themePreferences = remember { ThemePreferences(requireContext()) }
                var darkModeEnabled by remember {
                    mutableStateOf(themePreferences.darkModeEnabled)
                }
                val securityPreferences = remember { SecurityPreferences(requireContext()) }
                var biometricPinEnabled by remember {
                    mutableStateOf(securityPreferences.biometricPinEnabled)
                }

                SettingsScreen(
                    onBackClick = { findNavController().popBackStack() },
                    onModelsClick = {
                        findNavController().navigateFromSettings(R.id.action_settings_to_models)
                    },
                    onLanguageClick = {
                        findNavController().navigateFromSettings(R.id.action_settings_to_language)
                    },
                    onConversationHistoryClick = {
                        requireContext()
                            .getSharedPreferences("conversation_prefs", 0)
                            .edit()
                            .putBoolean("open_history_drawer", true)
                            .apply()
                        findNavController().popBackStack(R.id.nav_home, false)
                    },
                    modelsDetailContent = if (twoPane) {
                        { ModelsDetailPane() }
                    } else null,
                    languageDetailContent = if (twoPane) {
                        { LanguageContent() }
                    } else null,
                    masterWidth = masterWidth,
                    huggingFaceToken = huggingFaceToken,
                    onHuggingFaceTokenSave = { token ->
                        huggingFaceToken = token
                        onlinePreferences.huggingFaceToken = token
                        Toast.makeText(
                            requireContext(),
                            R.string.huggingface_token_saved,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    openRouterApiKey = openRouterApiKey,
                    onOpenRouterApiKeySave = { apiKey ->
                        openRouterApiKey = apiKey
                        onlinePreferences.openRouterApiKey = apiKey
                        Toast.makeText(
                            requireContext(),
                            R.string.openrouter_api_key_saved,
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                    darkModeEnabled = darkModeEnabled,
                    onDarkModeChange = { enabled ->
                        darkModeEnabled = enabled
                        themePreferences.darkModeEnabled = enabled
                        themePreferences.apply()
                    },
                    biometricPinEnabled = biometricPinEnabled,
                    onBiometricPinChange = { enabled ->
                        biometricPinEnabled = enabled
                        securityPreferences.biometricPinEnabled = enabled
                    },
                )
            }
        }
    }

    /**
     * Embedded Models pane — same observable surface that [ModelsFragment]
     * binds, just rendered without the Scaffold/topBar wrapper.
     */
    @androidx.compose.runtime.Composable
    private fun ModelsDetailPane() {
        val storageInfo by storageViewModel.storageInfo.observeAsState()
        val allModels by storageViewModel.allModels.observeAsState(emptyList())
        val downloadingProgress by storageViewModel.downloadingModels.observeAsState(emptyMap())
        val snackbarMessage by storageViewModel.snackbarMessage.observeAsState()
        val pendingMigration by storageViewModel.pendingMigration.observeAsState()
        val migrationProgress by storageViewModel.migrationProgress.observeAsState()

        // Refresh storage info when a download completes (mirrors the
        // LaunchedEffect that lives in ModelsFragment).
        val prevDownloadCount = remember { mutableIntStateOf(downloadingProgress.size) }
        LaunchedEffect(downloadingProgress.size) {
            if (downloadingProgress.size < prevDownloadCount.intValue) {
                storageViewModel.loadStorageInfo()
            }
            prevDownloadCount.intValue = downloadingProgress.size
        }
        // Settings doesn't call loadStorageInfo() at fragment-create time
        // (unlike ModelsFragment) because we don't know if Models will be
        // opened. Trigger a fetch when this pane first composes.
        LaunchedEffect(Unit) {
            storageViewModel.loadStorageInfo()
        }

        val snackbarHostState = remember { SnackbarHostState() }
        ModelsContent(
            storageInfo = storageInfo,
            allModels = allModels,
            downloadingModels = downloadingProgress,
            snackbarMessage = snackbarMessage,
            pendingMigration = pendingMigration,
            migrationProgress = migrationProgress,
            deviceLanguage = storageViewModel.deviceLanguage,
            snackbarHostState = snackbarHostState,
            onChangeFolderClick = {
                try {
                    folderPickerLauncher.launch(null)
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        R.string.folder_picker_unavailable,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            },
            onDeleteModel = { model -> storageViewModel.deleteModel(model) },
            onDownloadModel = { model -> startDownloadWithPermissionCheck(model) },
            onCancelDownload = { model -> storageViewModel.cancelDownload(model) },
            onSnackbarDismiss = { storageViewModel.clearSnackbar() },
            onConfirmMigration = { storageViewModel.confirmMigration() },
            onSkipMigration = { storageViewModel.skipMigration() },
            onCancelMigration = { storageViewModel.cancelMigration() },
        )
    }

}
