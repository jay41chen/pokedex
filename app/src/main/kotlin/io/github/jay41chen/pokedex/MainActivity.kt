package io.github.jay41chen.pokedex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.jay41chen.pokedex.ui.main.MainViewModel
import io.github.jay41chen.pokedex.ui.navigation.PokedexNavHost
import io.github.jay41chen.pokedex.ui.theme.PokedexTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

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
    }
}
