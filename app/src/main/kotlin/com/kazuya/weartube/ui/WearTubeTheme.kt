package com.kazuya.weartube.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

/** 赤（アイコンの色）をアクセントにした黒基調。プレーヤー画面はテーマを使わず #000000 で塗る。 */
object WearTubeColors {
    val Red = Color(0xFFD51618)
    val RedContainer = Color(0xFF7A0A0C)
    val Surface = Color(0xFF1C1B1F)
    val SurfaceHigh = Color(0xFF2B2A2E)
    val ProgressBlue = Color(0xFF3B8CFF)
}

private val colorScheme =
    ColorScheme(
        primary = WearTubeColors.Red,
        onPrimary = Color.White,
        primaryContainer = WearTubeColors.RedContainer,
        onPrimaryContainer = Color.White,
        secondary = Color(0xFFE0E0E0),
        onSecondary = Color.Black,
        background = Color.Black,
        onBackground = Color.White,
        surfaceContainer = WearTubeColors.Surface,
        surfaceContainerHigh = WearTubeColors.SurfaceHigh,
        onSurface = Color.White,
        onSurfaceVariant = Color(0xFFC7C7C7),
    )

@Composable
fun WearTubeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
