package com.kazuya.weartube.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kazuya.weartube.AppContainer
import com.kazuya.weartube.WearTubeApp

/** Hilt を使わないので、AppContainer を渡すファクトリをここで組む。 */
@Composable
inline fun <reified VM : ViewModel> containerViewModel(crossinline create: (WearTubeApp, AppContainer) -> VM): VM {
    val app = LocalContext.current.applicationContext as WearTubeApp
    return viewModel(
        factory =
            viewModelFactory {
                initializer { create(app, app.container) }
            },
    )
}
