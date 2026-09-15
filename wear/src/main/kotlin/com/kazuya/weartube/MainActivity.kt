package com.kazuya.weartube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kazuya.weartube.ui.WearTubeTheme
import com.kazuya.weartube.ui.navigation.WearTubeNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearTubeTheme {
                WearTubeNavHost()
            }
        }
    }
}
