package com.kazuya.weartube.ui.common

import android.app.RemoteInput
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.wear.input.RemoteInputIntentHelper

/**
 * Wear OS のシステム入力（キーボード／音声）で 1 行のテキストをもらう。
 * API キーや動画 URL の入力に使う。結果は [onResult]（空なら null）。
 */
@Composable
fun rememberTextInputLauncher(onResult: (String?) -> Unit): ManagedActivityResultLauncher<Intent, ActivityResult> =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        val text = data?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(KEY)?.toString() }
        onResult(text?.takeIf { it.isNotBlank() })
    }

/** [rememberTextInputLauncher] に渡す Intent。 */
fun textInputIntent(label: String): Intent {
    val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
    val remoteInput =
        RemoteInput
            .Builder(KEY)
            .setLabel(label)
            .setAllowFreeFormInput(true)
            .build()
    RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
    return intent
}

private const val KEY = "text"
