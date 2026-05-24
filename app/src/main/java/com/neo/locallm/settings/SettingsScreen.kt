@file:OptIn(ExperimentalMaterial3Api::class)

package com.neo.locallm.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neo.locallm.R
import com.neo.locallm.theme.PlaygroundTheme
import java.util.Locale

private enum class SettingsDetail { Models, Language }

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onModelsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onConversationHistoryClick: () -> Unit,
    openRouterApiKey: String = "",
    onOpenRouterApiKeySave: (String) -> Unit = {},
    darkModeEnabled: Boolean = false,
    onDarkModeChange: (Boolean) -> Unit = {},
    biometricPinEnabled: Boolean = false,
    onBiometricPinChange: (Boolean) -> Unit = {},
    modelsDetailContent: (@Composable () -> Unit)? = null,
    languageDetailContent: (@Composable () -> Unit)? = null,
    masterWidth: androidx.compose.ui.unit.Dp = 320.dp,
) {
    val configuration = LocalConfiguration.current
    val twoPane = configuration.screenWidthDp >= 840
    var detail by remember {
        mutableStateOf<SettingsDetail?>(
            if (modelsDetailContent != null) SettingsDetail.Models else null
        )
    }

    if (twoPane) {
        Row(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.width(masterWidth),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.settings)) },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back)
                                )
                            }
                        }
                    )
                }
            ) { masterPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(masterPadding)
                        .padding(start = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                ) {
                    SettingsList(
                        selectedDetail = detail,
                        onModelsClick = {
                            if (modelsDetailContent != null) detail = SettingsDetail.Models
                            else onModelsClick()
                        },
                        onLanguageClick = {
                            if (languageDetailContent != null) detail = SettingsDetail.Language
                            else onLanguageClick()
                        },
                        onConversationHistoryClick = onConversationHistoryClick,
                        openRouterApiKey = openRouterApiKey,
                        onOpenRouterApiKeySave = onOpenRouterApiKeySave,
                        darkModeEnabled = darkModeEnabled,
                        onDarkModeChange = onDarkModeChange,
                        biometricPinEnabled = biometricPinEnabled,
                        onBiometricPinChange = onBiometricPinChange,
                    )
                }
            }

            val detailTitle = when (detail) {
                SettingsDetail.Models -> stringResource(R.string.models)
                SettingsDetail.Language -> stringResource(R.string.language)
                null -> ""
            }
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { TopAppBar(title = { Text(detailTitle) }) }
            ) { detailPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(detailPadding)
                ) {
                    when (detail) {
                        SettingsDetail.Models -> modelsDetailContent?.invoke()
                        SettingsDetail.Language -> languageDetailContent?.invoke()
                        null -> Unit
                    }
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            SettingsList(
                selectedDetail = null,
                onModelsClick = onModelsClick,
                onLanguageClick = onLanguageClick,
                onConversationHistoryClick = onConversationHistoryClick,
                openRouterApiKey = openRouterApiKey,
                onOpenRouterApiKeySave = onOpenRouterApiKeySave,
                darkModeEnabled = darkModeEnabled,
                onDarkModeChange = onDarkModeChange,
                biometricPinEnabled = biometricPinEnabled,
                onBiometricPinChange = onBiometricPinChange,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun SettingsList(
    selectedDetail: SettingsDetail?,
    onModelsClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onConversationHistoryClick: () -> Unit,
    openRouterApiKey: String,
    onOpenRouterApiKeySave: (String) -> Unit,
    darkModeEnabled: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    biometricPinEnabled: Boolean,
    onBiometricPinChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKeyDraft by remember(openRouterApiKey) { mutableStateOf(openRouterApiKey) }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        ToggleRow(
            icon = Icons.Outlined.DarkMode,
            title = stringResource(R.string.dark_mode),
            subtitle = stringResource(R.string.dark_mode_subtitle),
            checked = darkModeEnabled,
            onCheckedChange = onDarkModeChange
        )

        val currentTag = currentLanguageTag()
        val languageSubtitle = if (currentTag == null) {
            stringResource(R.string.language_system_default)
        } else {
            Locale.forLanguageTag(currentTag).let { locale ->
                locale.getDisplayName(locale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
            }
        }
        SettingsRow(
            icon = Icons.Outlined.Language,
            title = stringResource(R.string.language),
            subtitle = languageSubtitle,
            selected = selectedDetail == SettingsDetail.Language,
            onClick = onLanguageClick
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.VpnKey,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    label = { Text(stringResource(R.string.openrouter_api_key)) },
                    supportingText = { Text(stringResource(R.string.openrouter_api_key_subtitle)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onOpenRouterApiKeySave(apiKeyDraft) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }

        SettingsRow(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.models),
            subtitle = stringResource(R.string.models_subtitle),
            selected = selectedDetail == SettingsDetail.Models,
            onClick = onModelsClick
        )

        SettingsRow(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.conversation_history),
            subtitle = stringResource(R.string.conversation_history_subtitle),
            onClick = onConversationHistoryClick
        )

        ToggleRow(
            icon = Icons.Outlined.Fingerprint,
            title = stringResource(R.string.biometric_pin),
            subtitle = stringResource(R.string.biometric_pin_subtitle),
            checked = biometricPinEnabled,
            onCheckedChange = onBiometricPinChange
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val background = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    PlaygroundTheme {
        SettingsScreen(
            onBackClick = {},
            onModelsClick = {},
            onLanguageClick = {},
            onConversationHistoryClick = {}
        )
    }
}
