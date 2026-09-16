package com.kazuya.weartube.ui.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.kazuya.weartube.ui.home.HomeScreen
import com.kazuya.weartube.ui.player.PlayerScreen
import com.kazuya.weartube.ui.poc.PocScreen
import com.kazuya.weartube.ui.search.SearchScreen
import com.kazuya.weartube.ui.settings.SettingsScreen

/** design.md 4.1 の画面遷移。右スワイプで戻る（Wear OS 標準）。 */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val PLAYER = "player"
    const val SETTINGS = "settings"

    /** 再生方式の検証（design.md 12 章）。本番の再生画面とは別。 */
    const val POC = "poc"
}

@Composable
fun WearTubeNavHost() {
    val navController = rememberSwipeDismissableNavController()
    // 時刻表示は全画面で残す（プレーヤーも没入表示にしない）
    AppScaffold(timeText = { TimeText() }) {
        SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    onSearch = { navController.navigate(Routes.SEARCH) },
                    onPlay = { navController.navigate(Routes.PLAYER) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onPlay = { navController.navigate(Routes.PLAYER) })
            }
            composable(Routes.PLAYER) {
                PlayerScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onPoc = { navController.navigate(Routes.POC) })
            }
            composable(Routes.POC) {
                PocScreen()
            }
        }
    }
}
