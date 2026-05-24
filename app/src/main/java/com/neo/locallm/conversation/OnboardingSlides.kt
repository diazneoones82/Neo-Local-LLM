package com.neo.locallm.conversation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neo.locallm.R

@Composable
fun FirstRunOnboarding() {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
    }
    var visible by remember { mutableStateOf(!prefs.getBoolean(KEY_ONBOARDING_SEEN, false)) }
    if (!visible) return

    val slides = listOf(
        OnboardingSlide(
            icon = Icons.Outlined.Download,
            title = stringResource(R.string.onboarding_download_title),
            body = stringResource(R.string.onboarding_download_body),
        ),
        OnboardingSlide(
            icon = Icons.Outlined.SmartToy,
            title = stringResource(R.string.onboarding_chat_title),
            body = stringResource(R.string.onboarding_chat_body),
        ),
        OnboardingSlide(
            icon = Icons.Outlined.CloudDone,
            title = stringResource(R.string.onboarding_online_title),
            body = stringResource(R.string.onboarding_online_body),
        ),
    )
    var index by remember { mutableIntStateOf(0) }
    val slide = slides[index]
    val isLast = index == slides.lastIndex

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 8.dp,
            ) {
                Icon(
                    imageVector = slide.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(28.dp)
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                text = slide.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = slide.body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                slides.indices.forEach { dotIndex ->
                    Surface(
                        shape = CircleShape,
                        color = if (dotIndex == index) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                    ) {
                        Spacer(Modifier.size(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
                        visible = false
                    }
                ) {
                    Text(stringResource(R.string.skip))
                }
                Button(
                    onClick = {
                        if (isLast) {
                            prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
                            visible = false
                        } else {
                            index += 1
                        }
                    }
                ) {
                    Text(
                        stringResource(
                            if (isLast) R.string.get_started else R.string.next
                        )
                    )
                }
            }
        }
    }
}

private data class OnboardingSlide(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
