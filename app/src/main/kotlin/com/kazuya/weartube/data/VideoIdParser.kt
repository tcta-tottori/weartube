package com.kazuya.weartube.data

/** 入力された文字列から YouTube の動画 ID（11 文字）を取り出す。URL でも ID 単体でもよい。 */
object VideoIdParser {
    private val idPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val urlPatterns =
        listOf(
            Regex("[?&]v=([A-Za-z0-9_-]{11})"),
            Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
            Regex("/(?:shorts|embed|live|v)/([A-Za-z0-9_-]{11})"),
        )

    fun parse(input: String): String? {
        val text = input.trim()
        if (idPattern.matches(text)) return text
        return urlPatterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.get(1) }
    }
}
