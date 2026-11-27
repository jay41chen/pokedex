package io.github.jay41chen.pokedex.data.remote

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.github.jay41chen.pokedex.data.sync.ConnectivityChecker

// [待確認] Uses INTERNET rather than VALIDATED so that captive portals and similar cases
// are not misjudged as offline.
internal class AndroidConnectivityChecker(private val cm: ConnectivityManager) : ConnectivityChecker {
    override fun isOnline(): Boolean =
        cm.getNetworkCapabilities(cm.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
}
