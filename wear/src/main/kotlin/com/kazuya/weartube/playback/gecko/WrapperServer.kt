package com.kazuya.weartube.playback.gecko

import android.util.Log
import com.kazuya.weartube.playback.PlaybackCapabilities
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * ラッパーページを返すだけの端末内 HTTP サーバー（design.md 12 章 STEP 5）。
 *
 * IFrame Player API は `file://` では origin を検証できない。ここから配ると
 * `http://127.0.0.1:<port>` という実体のある origin になり、埋め込み URL 直読みと比較できる。
 */
class WrapperServer(
    private val html: String,
) : Closeable {
    private val server = ServerSocket(0, BACKLOG, InetAddress.getByName(LOOPBACK))

    val origin: String get() = "http://$LOOPBACK:${server.localPort}"

    init {
        thread(name = "weartube-wrapper", isDaemon = true) { acceptLoop() }
        Log.i(PlaybackCapabilities.TAG, "WRAPPER_SERVER=$origin")
    }

    /** 動画 ID を渡して開く URL。 */
    fun urlFor(videoId: String): String = "$origin/player?v=$videoId"

    override fun close() {
        try {
            server.close()
        } catch (e: IOException) {
            Log.w(PlaybackCapabilities.TAG, "WRAPPER_SERVER_CLOSE=${e.message}")
        }
    }

    private fun acceptLoop() {
        while (!server.isClosed) {
            try {
                server.accept().use(::respond)
            } catch (e: IOException) {
                if (!server.isClosed) Log.w(PlaybackCapabilities.TAG, "WRAPPER_SERVER_ACCEPT=${e.message}")
                return
            }
        }
    }

    /** どのパスでも同じページを返す。動画 ID はクエリから JS が読む。 */
    private fun respond(socket: Socket) {
        val body = html.toByteArray(Charsets.UTF_8)
        socket.getInputStream().bufferedReader().readLine()
        socket.getOutputStream().apply {
            write(
                (
                    "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html; charset=utf-8\r\n" +
                        "Content-Length: ${body.size}\r\n" +
                        "Cache-Control: no-store\r\n" +
                        "Connection: close\r\n\r\n"
                ).toByteArray(Charsets.US_ASCII),
            )
            write(body)
            flush()
        }
    }

    private companion object {
        const val LOOPBACK = "127.0.0.1"
        const val BACKLOG = 4
    }
}
