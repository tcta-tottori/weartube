package com.kazuya.weartube.ui.search

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.kazuya.weartube.R
import com.kazuya.weartube.data.SearchError
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.ui.WearTubeTheme
import com.kazuya.weartube.ui.common.VideoListItem
import com.kazuya.weartube.ui.common.containerViewModel

/** F-02 音声検索。画面を開いたらすぐ音声入力を出し、結果を一覧にする。 */
@Composable
fun SearchScreen(onPlay: () -> Unit) {
    val viewModel = containerViewModel { _, c -> SearchViewModel(c.search, c.favorites, c.playQueue) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prompt = stringResource(R.string.search_prompt)

    val voiceLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) viewModel.search(spoken)
        }
    val startVoice = {
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ja-JP")
                .putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.voiceUnavailable()
        }
    }

    LaunchedEffect(Unit) {
        if (viewModel.isIdle) startVoice()
    }
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val res =
            when (m) {
                SearchMessage.FAVORITE_ADDED -> R.string.favorite_added
                SearchMessage.FAVORITE_REMOVED -> R.string.favorite_removed
                SearchMessage.VOICE_UNAVAILABLE -> R.string.search_voice_unavailable
            }
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    SearchContent(
        state = state,
        onRetry = startVoice,
        onPlay = { index ->
            viewModel.play(index)
            onPlay()
        },
        onToggleFavorite = viewModel::toggleFavorite,
    )
}

@Composable
private fun SearchContent(
    state: SearchUiState,
    onRetry: () -> Unit,
    onPlay: (Int) -> Unit,
    onToggleFavorite: (VideoItem) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = listState) {
        if (state.isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@ScreenScaffold
        }
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                ListHeader {
                    Text(
                        text = state.query.ifBlank { stringResource(R.string.search_title) },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val error = state.error
            when {
                error != null -> item { Message(errorText(error)) }
                !state.idle && state.results.isEmpty() -> item { Message(stringResource(R.string.search_empty)) }
            }
            itemsIndexed(state.results) { index, item ->
                VideoListItem(
                    item = item,
                    isFavorite = item.videoId in state.favoriteIds,
                    onClick = { onPlay(index) },
                    onLongClick = { onToggleFavorite(item) },
                )
            }
            // クォータ超過時は検索を止め、お気に入り再生のみに縮退する（CLAUDE.md エラー時の振る舞い）
            if (error != SearchError.QuotaExceeded && error != SearchError.NoApiKey) {
                item {
                    Button(
                        onClick = onRetry,
                        icon = { Icon(painter = painterResource(R.drawable.ic_mic), contentDescription = null) },
                        label = { Text(stringResource(R.string.search_retry)) },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun errorText(error: SearchError): String =
    stringResource(
        when (error) {
            SearchError.NoApiKey -> R.string.search_no_api_key
            SearchError.QuotaExceeded -> R.string.search_quota
            SearchError.InvalidKey -> R.string.search_key_invalid
            SearchError.Network -> R.string.search_network
            SearchError.Unknown -> R.string.search_failed
        },
    )

@Composable
private fun Message(text: String) {
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
private fun SearchPreview() {
    WearTubeTheme {
        SearchContent(
            state =
                SearchUiState(
                    query = "スイス アルプス",
                    idle = false,
                    results =
                        listOf(
                            VideoItem("dQw4w9WgXcQ", "スイス・アルプスの絶景 | 4K 空撮", "Nature Channel"),
                            VideoItem("abc123defgh", "アルプス縦走ライブ", "Live Channel", isLive = true),
                        ),
                    favoriteIds = setOf("dQw4w9WgXcQ"),
                ),
            onRetry = {},
            onPlay = {},
            onToggleFavorite = {},
        )
    }
}
