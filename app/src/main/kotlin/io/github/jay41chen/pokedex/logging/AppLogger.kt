package io.github.jay41chen.pokedex.logging

import android.util.Log

/** :app 的 log 介面；VM 透過它記錄錯誤，JVM 測試換成 fake。形式同 :data 的 DataLogger（該介面是 :data internal，不能共用）。 */
fun interface AppLogger {
    fun warn(message: String, cause: Throwable?)
}

object AndroidAppLogger : AppLogger {
    private const val TAG = "PokedexApp"

    override fun warn(message: String, cause: Throwable?) {
        Log.w(TAG, message, cause)
    }
}
