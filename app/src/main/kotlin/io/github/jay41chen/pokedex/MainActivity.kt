package io.github.jay41chen.pokedex

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.jay41chen.pokedex.logging.AppLogger
import io.github.jay41chen.pokedex.ui.main.MainViewModel
import io.github.jay41chen.pokedex.ui.navigation.PokedexNavHost
import io.github.jay41chen.pokedex.ui.theme.PokedexTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @Inject lateinit var logger: AppLogger

    private var networkCallbackRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                mainViewModel.onNetworkAvailable()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        setContent {
            PokedexTheme {
                PokedexNavHost()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.onAppForegrounded()
        try {
            getSystemService(ConnectivityManager::class.java)
                .registerDefaultNetworkCallback(networkCallback, Handler(Looper.getMainLooper()))
            networkCallbackRegistered = true
        } catch (e: RuntimeException) {
            logger.warn("Network callback register failed", e)
        }
    }

    override fun onStop() {
        super.onStop()
        if (networkCallbackRegistered) {
            try {
                getSystemService(ConnectivityManager::class.java).unregisterNetworkCallback(networkCallback)
            } catch (e: IllegalArgumentException) {
                logger.warn("Network callback unregister failed", e)
            }
            networkCallbackRegistered = false
        }
    }
}
