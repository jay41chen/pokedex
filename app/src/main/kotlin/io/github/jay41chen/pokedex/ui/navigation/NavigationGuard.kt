package io.github.jay41chen.pokedex.ui.navigation

import androidx.lifecycle.Lifecycle

/**
 * 只在 lifecycle 為 RESUMED 時執行 [action]；轉場中（離開中的畫面已不是 RESUMED）的重複點擊會被丟掉，避免連點把 back stack 彈空或重複導頁。
 *
 * 語意同官方 `androidx.lifecycle.compose.dropUnlessResumed`；自己寫是因為它只接受無參數的 lambda，而 `onPokemonClick` 要帶 `PokemonId`。
 * 已知取捨（同官方）：剛返回的畫面在進場動畫結束前還不是 RESUMED，這段期間（實測約 0.5–1 秒）的點擊也會被丟掉。
 */
internal fun Lifecycle.runIfResumed(action: () -> Unit) {
    if (currentState.isAtLeast(Lifecycle.State.RESUMED)) action()
}
