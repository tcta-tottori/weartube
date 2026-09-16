package com.kazuya.weartube.poc

/**
 * GeckoView で何を読み込むか。error 153 が出たときに GeckoView 自体の問題か
 * referer / origin の問題かを切り分けるため分けている（design.md 12 章）。
 */
enum class GeckoLoadMode {
    /** YouTube の埋め込みページを直接開く。referer に https://www.youtube.com/ を付ける。 */
    DIRECT_EMBED,

    /** 端末内の HTTP サーバーが返すラッパーページから IFrame Player API を使う。 */
    LOCAL_WRAPPER,
}
