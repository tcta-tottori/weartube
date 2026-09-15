package com.kazuya.weartube.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** アイコンの赤をアクセントにしたライトテーマ。 */
private val colorScheme =
    lightColorScheme(
        primary = Color(0xFFD51618),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDAD6),
        onPrimaryContainer = Color(0xFF410002),
        secondary = Color(0xFF775652),
        surface = Color(0xFFFFFBFF),
        background = Color(0xFFFFFBFF),
    )

@Composable
fun MobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}
