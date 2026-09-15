package com.kazuya.weartube.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.kazuya.weartube.BuildConfig
import com.kazuya.weartube.R
import com.kazuya.weartube.audio.AudioOutput
import com.kazuya.weartube.data.VideoItem
import com.kazuya.weartube.player.PlaybackStatus
import com.kazuya.weartube.ui.WearTubeColors
import com.kazuya.weartube.ui.WearTubeTheme
import com.kazuya.weartube.ui.common.containerViewModel
import com.kazuya.weartube.ui.common.formatTime
import kotlinx.coroutines.delay

/** design.md 4.2 のプレーヤー画面。全面黒、動画は幅 100% の 16:9、コントロールはタップで出し入れ。 */
@Composable
fun PlayerScreen(onBack: () -> Unit) {
    val viewModel = containerViewModel { app, c -> PlayerViewModel(app, c) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 再生中は画面を消さない。画面を離れるときは必ず clear する
    val keepScreenOn = state.isPlaying && state.error == null
    DisposableEffect(keepScreenOn) {
        val window = context.findActivity()?.window
        if (keepScreenOn) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    PlayerContent(
        state = state,
        webView = viewModel.webView,
        callbacks =
            PlayerCallbacks(
                onTogglePlay = viewModel::togglePlay,
                onPrev = viewModel::prev,
                onNext = viewModel::next,
                onSeek = viewModel::seekToFraction,
                onToggleFavorite = viewModel::toggleFavorite,
                onRotary = viewModel.volume::onRotary,
                onRetry = viewModel::retry,
                onBack = onBack,
            ),
    )
}

class PlayerCallbacks(
    val onTogglePlay: () -> Unit,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onToggleFavorite: () -> Unit,
    val onRotary: (Float) -> Unit,
    val onRetry: () -> Unit,
    val onBack: () -> Unit,
)

@Composable
private fun PlayerContent(
    state: PlayerUiState,
    webView: WebView?,
    callbacks: PlayerCallbacks,
) {
    var overlayVisible by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }
    val touch: () -> Unit = { interaction++ }

    // 3 秒無操作で自動的に隠す
    LaunchedEffect(overlayVisible, interaction) {
        if (overlayVisible) {
            delay(AUTO_HIDE_MILLIS)
            overlayVisible = false
        }
    }
    // 回転リューズ＝メディア音量。フォーカスを持っていないとイベントが来ない
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onRotaryScrollEvent { event ->
                    callbacks.onRotary(event.verticalScrollPixels)
                    true
                }.focusRequester(focusRequester)
                .focusable(),
    ) {
        val width = maxWidth
        val videoHeight = width * 9f / 16f
        val band = (maxHeight - videoHeight) / 2

        // 動画本体。左右が円で切れるのは仕様（縮めない）
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
        ) {
            if (webView != null && state.error == null) {
                AndroidView(
                    factory = { _ -> webView.also { view -> (view.parent as? ViewGroup)?.removeView(view) } },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // WebView へタップを通さず、ここでコントロールの表示／非表示を切り替える
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            overlayVisible = !overlayVisible
                            touch()
                        }
                    },
        )

        if (state.error == null && state.isLoading && !overlayVisible) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(32.dp))
        }

        AnimatedVisibility(
            visible = overlayVisible && state.error == null,
            enter = fadeIn(tween(FADE_MILLIS)),
            exit = fadeOut(tween(FADE_MILLIS)),
        ) {
            PlayerOverlay(state = state, width = width, videoHeight = videoHeight, band = band, callbacks = callbacks, onTouch = touch)
        }

        if (state.volumeWarning && state.error == null) {
            Text(
                text = stringResource(R.string.player_volume_zero),
                style = MaterialTheme.typography.labelSmall,
                color = WearTubeColors.Red,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).offset(y = videoHeight / 2 + 30.dp),
            )
        }

        state.error?.let {
            ErrorView(error = it, detail = state.errorDetail, onRetry = callbacks.onRetry, onBack = callbacks.onBack)
        }
    }
}

/** コントロール一式。全面スクリムは敷かず、ボタンごとに白 35% の円を敷く。 */
@Composable
private fun PlayerOverlay(
    state: PlayerUiState,
    width: Dp,
    videoHeight: Dp,
    band: Dp,
    callbacks: PlayerCallbacks,
    onTouch: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // タイトル: 動画上端の黒帯、中央揃え・1 行・末尾省略
        Text(
            text = state.title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (band - TITLE_OFFSET).coerceAtLeast(0.dp))
                    .padding(horizontal = width * 0.08f)
                    .fillMaxWidth(),
        )

        // ボタン: 垂直中央、水平 22% / 50% / 78%
        val sideSize = width * SIDE_BUTTON_RATIO
        val centerSize = width * CENTER_BUTTON_RATIO
        RoundButton(
            icon = R.drawable.ic_skip_previous,
            contentDescription = stringResource(R.string.player_prev),
            size = sideSize,
            enabled = state.hasPrev,
            onClick = {
                onTouch()
                callbacks.onPrev()
            },
            modifier = Modifier.align(Alignment.Center).offset(x = width * (0.22f - 0.5f)),
        )
        RoundButton(
            icon = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
            contentDescription = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
            size = centerSize,
            enabled = true,
            onClick = {
                onTouch()
                callbacks.onTogglePlay()
            },
            modifier = Modifier.align(Alignment.Center),
        )
        RoundButton(
            icon = R.drawable.ic_skip_next,
            contentDescription = stringResource(R.string.player_next),
            size = sideSize,
            enabled = state.hasNext,
            onClick = {
                onTouch()
                callbacks.onNext()
            },
            modifier = Modifier.align(Alignment.Center).offset(x = width * (0.78f - 0.5f)),
        )

        // シークバー: 動画の下端付近に重ねる。ライブ配信では出さない
        if (!state.isLive) {
            SeekBar(
                progress = state.progress,
                onSeek = { fraction ->
                    onTouch()
                    callbacks.onSeek(fraction)
                },
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .offset(y = videoHeight / 2 - SEEKBAR_OFFSET)
                        .padding(horizontal = width * 0.08f)
                        .fillMaxWidth(),
            )
        }

        // 時間表示（経過 / 全体）: 動画外の黒帯に中央揃え。左に出力先アイコン、右にお気に入り
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.Center).offset(y = videoHeight / 2 + TIME_OFFSET),
        ) {
            Icon(
                painter =
                    painterResource(
                        if (state.output ==
                            AudioOutput.BLUETOOTH
                        ) {
                            R.drawable.ic_bluetooth_audio
                        } else {
                            R.drawable.ic_speaker
                        },
                    ),
                contentDescription =
                    stringResource(
                        if (state.output == AudioOutput.BLUETOOTH) R.string.player_output_bluetooth else R.string.player_output_speaker,
                    ),
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text =
                    if (state.isLive) {
                        stringResource(
                            R.string.search_live,
                        )
                    } else {
                        "${formatTime(state.currentSec)} / ${formatTime(state.durationSec)}"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = if (state.isLive) WearTubeColors.Red else Color.White,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Icon(
                painter = painterResource(if (state.isFavorite) R.drawable.ic_star else R.drawable.ic_star_outline),
                contentDescription = stringResource(R.string.player_favorite),
                tint = if (state.isFavorite) WearTubeColors.Red else Color.White.copy(alpha = 0.7f),
                modifier =
                    Modifier
                        .size(28.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            onTouch()
                            callbacks.onToggleFavorite()
                        }.padding(6.dp),
            )
        }
    }
}

/** 白 35% の円にアイコン。無効時は 30% の不透明度。タップ領域は最小 48dp を確保する。 */
@Composable
private fun RoundButton(
    icon: Int,
    contentDescription: String,
    size: Dp,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(maxOf(size, MIN_TOUCH))
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = BUTTON_BACKGROUND_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Composable
private fun BoxScope.ErrorView(
    error: PlayerError,
    detail: String?,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val message =
        stringResource(
            when (error) {
                PlayerError.OFFLINE -> R.string.player_error_offline
                PlayerError.NO_WEBVIEW -> R.string.player_error_no_webview
                PlayerError.UNPLAYABLE -> R.string.player_error_unplayable
                PlayerError.LOAD_FAILED -> R.string.player_error_load
            },
        )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
        if (detail != null) {
            // 実機でしか再現しないので、原因をそのまま出して写真で確認できるようにする
            Text(
                text = "${BuildConfig.VERSION_NAME} $detail",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = DETAIL_ALPHA),
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Row(modifier = Modifier.padding(top = 8.dp)) {
            if (error == PlayerError.OFFLINE || error == PlayerError.LOAD_FAILED) {
                Button(
                    onClick = onRetry,
                    label = { Text(stringResource(R.string.search_retry)) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Button(
                onClick = onBack,
                label = { Text(stringResource(R.string.player_back)) },
                colors = ButtonDefaults.buttonColors(),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private const val AUTO_HIDE_MILLIS = 3_000L
private const val FADE_MILLIS = 200
private const val CENTER_BUTTON_RATIO = 0.21f
private const val SIDE_BUTTON_RATIO = 0.15f
private const val BUTTON_BACKGROUND_ALPHA = 0.35f
private const val DISABLED_ALPHA = 0.3f
private const val DETAIL_ALPHA = 0.6f
private val MIN_TOUCH = 48.dp
private val TITLE_OFFSET = 20.dp
private val SEEKBAR_OFFSET = 12.dp
private val TIME_OFFSET = 14.dp

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun PlayerPreview() {
    WearTubeTheme {
        PlayerContent(
            state =
                PlayerUiState(
                    item = VideoItem("dQw4w9WgXcQ", "スイス・アルプスの絶景 | 4K 空撮", "Nature Channel"),
                    title = "スイス・アルプスの絶景 | 4K 空撮",
                    status = PlaybackStatus.PAUSED,
                    currentSec = 201.0,
                    durationSec = 754.0,
                    hasNext = true,
                ),
            webView = null,
            callbacks =
                PlayerCallbacks(
                    onTogglePlay = {},
                    onPrev = {},
                    onNext = {},
                    onSeek = {},
                    onToggleFavorite = {},
                    onRotary = {},
                    onRetry = {},
                    onBack = {},
                ),
        )
    }
}
