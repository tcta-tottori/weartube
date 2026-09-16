package com.kazuya.weartube.ui.poc

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.kazuya.weartube.R
import com.kazuya.weartube.playback.GeckoLoadMode
import com.kazuya.weartube.playback.PlaybackCapabilities
import com.kazuya.weartube.playback.PlaybackRoute
import com.kazuya.weartube.playback.RouteStatus
import com.kazuya.weartube.ui.WearTubeTheme
import com.kazuya.weartube.ui.common.containerViewModel

/**
 * 再生方式の検証画面（design.md 12 章）。本番の再生画面とは独立している。
 * ここで出る値は Logcat（タグ WearTubePoC）にも同じものが出る。
 */
@Composable
fun PocScreen() {
    val viewModel = containerViewModel { app, c -> PocViewModel(app, c.favorites) }
    val videoId by viewModel.videoId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val failedMessage = stringResource(R.string.poc_browser_failed)

    PocContent(
        capabilities = viewModel.capabilities,
        statuses = viewModel.statuses,
        videoId = videoId,
        onOpenBrowser = {
            if (!viewModel.openInWatchBrowser(context)) {
                Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
            }
        },
        onOpenGecko = { mode -> viewModel.openGecko(context, mode) },
    )
}

@Composable
private fun PocContent(
    capabilities: PlaybackCapabilities,
    statuses: List<RouteStatus>,
    videoId: String,
    onOpenBrowser: () -> Unit,
    onOpenGecko: (GeckoLoadMode) -> Unit,
) {
    val listState = rememberScalingLazyListState()
    val browserAvailable = statuses.firstOrNull { it.route == PlaybackRoute.WatchBrowser }?.available == true
    ScreenScaffold(scrollState = listState) {
        ScalingLazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { ListHeader { Text(stringResource(R.string.poc_title)) } }
            item { Line(stringResource(R.string.poc_webview, yesNo(capabilities.hasWebViewFeature))) }
            item { Line(stringResource(R.string.poc_browser, yesNo(capabilities.hasBrowserHandler))) }
            item { Line(stringResource(R.string.poc_custom_tabs, yesNo(capabilities.hasCustomTabsProvider))) }
            item {
                Line(
                    stringResource(
                        R.string.poc_screen,
                        capabilities.screenWidthPx,
                        capabilities.screenHeightPx,
                        capabilities.screenMinCssPx,
                    ),
                )
            }
            item { Line(stringResource(R.string.poc_env, capabilities.abis.firstOrNull().orEmpty(), capabilities.apiLevel)) }
            item { Line(stringResource(R.string.poc_video, videoId)) }
            item {
                Button(
                    onClick = onOpenBrowser,
                    enabled = browserAvailable,
                    label = { Text(stringResource(R.string.poc_route_browser), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = { onOpenGecko(GeckoLoadMode.LOCAL_WRAPPER) },
                    label = { Text(stringResource(R.string.poc_route_gecko_wrapper), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.buttonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Button(
                    onClick = { onOpenGecko(GeckoLoadMode.DIRECT_EMBED) },
                    label = { Text(stringResource(R.string.poc_route_gecko_embed), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item { Line(stringResource(R.string.poc_route_phone)) }
        }
    }
}

@Composable
private fun yesNo(value: Boolean): String = stringResource(if (value) R.string.poc_yes else R.string.poc_no)

@Composable
private fun Line(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun PocPreview() {
    WearTubeTheme {
        PocContent(
            capabilities =
                PlaybackCapabilities(
                    hasWebViewFeature = false,
                    hasBrowserHandler = false,
                    hasCustomTabsProvider = false,
                    handlerPackages = emptyList(),
                    abis = listOf("arm64-v8a"),
                    apiLevel = 34,
                    screenWidthPx = 450,
                    screenHeightPx = 450,
                    density = 2f,
                ),
            statuses = emptyList(),
            videoId = "aqz-KE-bpKQ",
            onOpenBrowser = {},
            onOpenGecko = {},
        )
    }
}
