package com.kazuya.weartube.ui.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.kazuya.weartube.R
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.ui.WearTubeTheme
import com.kazuya.weartube.ui.common.VideoListItem
import com.kazuya.weartube.ui.common.containerViewModel
import com.kazuya.weartube.ui.common.rememberTextInputLauncher
import com.kazuya.weartube.ui.common.textInputIntent

/** design.md 4.1 のホーム。先頭に検索チップ、続いてお気に入り一覧、最後に設定。 */
@Composable
fun HomeScreen(
    onSearch: () -> Unit,
    onPlay: () -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel = containerViewModel { _, c -> HomeViewModel(c.favorites, c.settings, c.playQueue) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val inputPrompt = stringResource(R.string.video_id_prompt)
    val inputLauncher =
        rememberTextInputLauncher { text ->
            if (text != null && viewModel.playVideoInput(text)) onPlay()
        }

    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val res =
            when (m) {
                HomeMessage.FAVORITE_REMOVED -> R.string.favorite_removed
                HomeMessage.VIDEO_ID_INVALID -> R.string.video_id_invalid
            }
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    HomeContent(
        state = state,
        onSearch = onSearch,
        onPlayFavorite = { index ->
            viewModel.playFavorite(index)
            onPlay()
        },
        onRemoveFavorite = viewModel::removeFavorite,
        onEnterVideoId = { inputLauncher.launch(textInputIntent(inputPrompt)) },
        onSettings = onSettings,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onSearch: () -> Unit,
    onPlayFavorite: (Int) -> Unit,
    onRemoveFavorite: (VideoItem) -> Unit,
    onEnterVideoId: () -> Unit,
    onSettings: () -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Image(
                        painter = painterResource(R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            item {
                Button(
                    onClick = onSearch,
                    enabled = state.hasApiKey,
                    icon = { Icon(painter = painterResource(R.drawable.ic_mic), contentDescription = null) },
                    label = { Text(stringResource(R.string.home_search)) },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!state.hasApiKey) {
                item { Hint(stringResource(R.string.home_no_api_key)) }
            }
            item { ListHeader { Text(stringResource(R.string.home_favorites)) } }
            if (state.favorites.isEmpty()) {
                item { Hint(stringResource(R.string.home_favorites_empty)) }
                item { Hint(stringResource(R.string.home_favorites_hint)) }
            }
            itemsIndexed(state.favorites) { index, item ->
                VideoListItem(
                    item = item,
                    isFavorite = true,
                    onClick = { onPlayFavorite(index) },
                    onLongClick = { onRemoveFavorite(item) },
                )
            }
            item {
                Button(
                    onClick = onEnterVideoId,
                    icon = { Icon(painter = painterResource(R.drawable.ic_link), contentDescription = null) },
                    label = { Text(stringResource(R.string.home_enter_id)) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
            item {
                Button(
                    onClick = onSettings,
                    icon = { Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = null) },
                    label = { Text(stringResource(R.string.home_settings)) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HomePreview() {
    WearTubeTheme {
        HomeContent(
            state =
                HomeUiState(
                    favorites =
                        listOf(
                            VideoItem("dQw4w9WgXcQ", "スイス・アルプスの絶景 | 4K 空撮", "Nature Channel"),
                            VideoItem("abc123defgh", "ライブ配信", "Live Channel", isLive = true),
                        ),
                ),
            onSearch = {},
            onPlayFavorite = {},
            onRemoveFavorite = {},
            onEnterVideoId = {},
            onSettings = {},
        )
    }
}
