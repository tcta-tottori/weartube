package com.kazuya.weartube.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** 再生や検索の前に「インターネットに出られるか」だけを見る。Wi-Fi / LTE のほか、スマホ経由の BT プロキシも含む。 */
class Connectivity(
    context: Context,
) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun isOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        val caps = manager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
