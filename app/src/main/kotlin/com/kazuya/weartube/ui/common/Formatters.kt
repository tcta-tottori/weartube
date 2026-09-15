package com.kazuya.weartube.ui.common

import java.util.Locale

/** 秒を `mm:ss`（1 時間以上は `h:mm:ss`）にする。 */
fun formatTime(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        String.format(Locale.JAPAN, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.JAPAN, "%02d:%02d", m, s)
    }
}
