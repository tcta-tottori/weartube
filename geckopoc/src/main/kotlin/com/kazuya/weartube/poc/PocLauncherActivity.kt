package com.kazuya.weartube.poc

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * 検証の入口。UI は作り込まず、読み込み方式を選んで [GeckoPlayerActivity] を開くだけ。
 *
 * 動画を変えるときは adb から渡せる:
 * `adb shell am start -n com.kazuya.weartube.poc/.PocLauncherActivity -e videoId <ID>`
 */
class PocLauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoId =
            intent
                .getStringExtra(GeckoPlayerActivity.EXTRA_VIDEO_ID)
                .orEmpty()
                .ifBlank { GeckoPlayerActivity.DEFAULT_VIDEO_ID }
        logDevice()

        val metrics = resources.displayMetrics
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(PADDING, PADDING * 3, PADDING, PADDING * 3)
                addView(
                    label(
                        getString(
                            R.string.device,
                            Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                            Build.VERSION.SDK_INT,
                            metrics.widthPixels,
                            metrics.heightPixels,
                        ),
                    ),
                )
                addView(label(getString(R.string.video, videoId)))
                addView(button(getString(R.string.mode_wrapper)) { start(videoId, GeckoLoadMode.LOCAL_WRAPPER) })
                addView(button(getString(R.string.mode_embed)) { start(videoId, GeckoLoadMode.DIRECT_EMBED) })
            }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun start(
        videoId: String,
        mode: GeckoLoadMode,
    ) {
        startActivity(
            Intent(this, GeckoPlayerActivity::class.java)
                .putExtra(GeckoPlayerActivity.EXTRA_VIDEO_ID, videoId)
                .putExtra(GeckoPlayerActivity.EXTRA_MODE, mode.name),
        )
    }

    private fun logDevice() {
        val metrics = resources.displayMetrics
        Log.i(GeckoRuntimeHolder.TAG, "ABIS=${Build.SUPPORTED_ABIS.joinToString(",")}")
        Log.i(GeckoRuntimeHolder.TAG, "API_LEVEL=${Build.VERSION.SDK_INT} MODEL=${Build.MODEL}")
        Log.i(
            GeckoRuntimeHolder.TAG,
            "SCREEN_PX=${metrics.widthPixels}x${metrics.heightPixels} DENSITY=${metrics.density} " +
                "SCREEN_MIN_CSS_PX=${(minOf(metrics.widthPixels, metrics.heightPixels) / metrics.density).toInt()}",
        )
    }

    private fun label(text: String): TextView =
        TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = LABEL_SP
            setPadding(0, 0, 0, PADDING)
        }

    private fun button(
        text: String,
        onClick: () -> Unit,
    ): Button =
        Button(this).apply {
            this.text = text
            layoutParams =
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setOnClickListener { onClick() }
        }

    private companion object {
        const val PADDING = 12
        const val LABEL_SP = 12f
    }
}
