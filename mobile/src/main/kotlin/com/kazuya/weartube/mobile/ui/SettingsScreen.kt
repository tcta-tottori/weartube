package com.kazuya.weartube.mobile.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.kazuya.weartube.data.ApiKeySource
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.mobile.BuildConfig
import com.kazuya.weartube.mobile.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** スマホ側の唯一の画面。API キー、時計との同期、お気に入りの管理。 */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val res =
            when (m) {
                Message.API_KEY_SAVED -> R.string.api_key_saved
                Message.SYNC_DONE -> R.string.sync_done
                Message.SYNC_FAILED -> R.string.sync_failed
                Message.FAVORITE_ADDED -> R.string.favorite_added
                Message.FAVORITE_EXISTS -> R.string.favorite_exists
                Message.FAVORITE_REMOVED -> R.string.favorite_removed
                Message.VIDEO_ID_INVALID -> R.string.video_id_invalid
            }
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    SettingsContent(
        state = state,
        onSaveApiKey = viewModel::saveApiKey,
        onClearApiKey = viewModel::clearApiKey,
        onSyncNow = viewModel::syncNow,
        onAddFavorite = viewModel::addFavoriteFromText,
        onRemoveFavorite = viewModel::removeFavorite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onSyncNow: () -> Unit,
    onAddFavorite: (String) -> Unit,
    onRemoveFavorite: (VideoItem) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(stringResource(R.string.title), modifier = Modifier.padding(start = 10.dp))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.intro), style = MaterialTheme.typography.bodyMedium) }
            item { ApiKeyCard(state, onSaveApiKey, onClearApiKey) }
            item { SyncCard(state, onSyncNow) }
            item { AddFavoriteCard(state, onAddFavorite) }
            if (state.favorites.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.favorites_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.favorites, key = { it.videoId }) { item -> FavoriteRow(item, onRemove = { onRemoveFavorite(item) }) }
            item {
                Text(
                    stringResource(R.string.version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ApiKeyCard(
    state: SettingsUiState,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.api_key_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text =
                    when (state.apiKeySource) {
                        ApiKeySource.USER -> stringResource(R.string.api_key_set, state.apiKeyTail)
                        ApiKeySource.BUILD_CONFIG -> stringResource(R.string.api_key_build)
                        ApiKeySource.NONE -> stringResource(R.string.api_key_unset)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.api_key_label)) },
                placeholder = { Text(stringResource(R.string.api_key_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        onSave(input)
                        input = ""
                    },
                    enabled = input.isNotBlank(),
                ) { Text(stringResource(R.string.api_key_save)) }
                if (state.apiKeySource == ApiKeySource.USER) {
                    OutlinedButton(onClick = onClear) { Text(stringResource(R.string.api_key_clear)) }
                }
            }
        }
    }
}

@Composable
private fun SyncCard(
    state: SettingsUiState,
    onSyncNow: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.sync_title), style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (state.watchConnected) R.string.sync_connected else R.string.sync_disconnected),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = syncLabel(state.lastSyncAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Button(onClick = onSyncNow, enabled = !state.busy) { Text(stringResource(R.string.sync_now)) }
                if (state.busy) {
                    CircularProgressIndicator(modifier = Modifier.padding(start = 12.dp).size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun AddFavoriteCard(
    state: SettingsUiState,
    onAdd: (String) -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.favorites_title, state.favorites.size), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.favorites_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.favorites_url_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Button(
                onClick = {
                    onAdd(input)
                    input = ""
                },
                enabled = input.isNotBlank() && !state.busy,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Icon(painterResource(R.drawable.ic_link), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.favorites_add))
            }
        }
    }
}

@Composable
private fun FavoriteRow(
    item: VideoItem,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Box(
                modifier =
                    Modifier
                        .width(96.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black),
            ) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        painterResource(R.drawable.ic_play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp).align(Alignment.Center),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text(
                    text = item.title.ifBlank { item.videoId },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val sub = if (item.isLive) stringResource(R.string.live) else item.channelTitle
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (item.isLive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.favorites_delete))
            }
        }
    }
}

@Composable
private fun syncLabel(lastSyncAt: Long?): String {
    val at = lastSyncAt ?: return stringResource(R.string.sync_never)
    val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())
    return stringResource(R.string.sync_last, time.format(DateTimeFormatter.ofPattern("M/d HH:mm")))
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    MobileTheme {
        SettingsContent(
            state =
                SettingsUiState(
                    apiKeySource = ApiKeySource.USER,
                    apiKeyTail = "Ab12",
                    favorites = listOf(VideoItem("dQw4w9WgXcQ", "スイス・アルプスの絶景 | 4K 空撮", "Nature Channel")),
                    watchConnected = true,
                ),
            onSaveApiKey = {},
            onClearApiKey = {},
            onSyncNow = {},
            onAddFavorite = {},
            onRemoveFavorite = {},
        )
    }
}
