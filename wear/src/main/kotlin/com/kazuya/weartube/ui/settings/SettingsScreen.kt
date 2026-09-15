package com.kazuya.weartube.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.kazuya.weartube.BuildConfig
import com.kazuya.weartube.R
import com.kazuya.weartube.data.ApiKeySource
import com.kazuya.weartube.ui.WearTubeTheme
import com.kazuya.weartube.ui.common.containerViewModel
import com.kazuya.weartube.ui.common.rememberTextInputLauncher
import com.kazuya.weartube.ui.common.textInputIntent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** design.md 4.1 の設定。APIキーの入力と電池の注意書き。 */
@Composable
fun SettingsScreen() {
    val viewModel = containerViewModel { _, c -> SettingsViewModel(c.settings, c.favorites) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prompt = stringResource(R.string.settings_api_key_prompt)
    val launcher = rememberTextInputLauncher { text -> if (text != null) viewModel.setApiKey(text) }

    SettingsContent(
        state = state,
        onEnterApiKey = { launcher.launch(textInputIntent(prompt)) },
        onClearApiKey = viewModel::clearApiKey,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onEnterApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { ListHeader { Text(stringResource(R.string.settings_title)) } }
            item {
                Button(
                    onClick = onEnterApiKey,
                    icon = { Icon(painter = painterResource(R.drawable.ic_key), contentDescription = null) },
                    label = { Text(stringResource(R.string.settings_api_key), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = { Text(apiKeyLabel(state), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.apiKeySource == ApiKeySource.USER) {
                item {
                    Button(
                        onClick = onClearApiKey,
                        icon = { Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null) },
                        label = { Text(stringResource(R.string.settings_api_key_clear), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        colors = ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item { Note(stringResource(R.string.settings_favorites_count, state.favoriteCount)) }
            item { Note(syncLabel(state.lastSyncAt)) }
            item { Note(stringResource(R.string.settings_battery_note)) }
            item { Note(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) }
        }
    }
}

@Composable
private fun apiKeyLabel(state: SettingsUiState): String =
    when (state.apiKeySource) {
        ApiKeySource.USER -> stringResource(R.string.settings_api_key_set, state.apiKeyTail)
        ApiKeySource.BUILD_CONFIG -> stringResource(R.string.settings_api_key_build)
        ApiKeySource.NONE -> stringResource(R.string.settings_api_key_unset)
    }

@Composable
private fun syncLabel(lastSyncAt: Long?): String {
    val at = lastSyncAt ?: return stringResource(R.string.settings_not_synced)
    val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())
    return stringResource(R.string.settings_synced_at, time.format(DateTimeFormatter.ofPattern("M/d HH:mm")))
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SettingsPreview() {
    WearTubeTheme {
        SettingsContent(
            state = SettingsUiState(apiKeySource = ApiKeySource.USER, apiKeyTail = "Ab12", favoriteCount = 3),
            onEnterApiKey = {},
            onClearApiKey = {},
        )
    }
}
