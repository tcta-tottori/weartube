package com.kazuya.weartube.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kazuya.weartube.mobile.ui.MobileTheme
import com.kazuya.weartube.mobile.ui.SettingsScreen
import com.kazuya.weartube.mobile.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SettingsViewModel by viewModels { SettingsViewModel.factory((application as MobileApp).container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleShare(intent)
        setContent {
            MobileTheme {
                SettingsScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShare(intent)
    }

    /** YouTube アプリの「共有」で受け取った URL をお気に入りに追加する。 */
    private fun handleShare(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        viewModel.addFavoriteFromText(text)
        // 同じ Intent を回転などで二重処理しない
        intent.action = null
    }
}
