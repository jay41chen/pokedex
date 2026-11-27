package io.github.jay41chen.pokedex.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.jay41chen.pokedex.ui.detail.DetailScreen
import io.github.jay41chen.pokedex.ui.detail.DetailViewModel
import io.github.jay41chen.pokedex.ui.home.HomeScreen
import io.github.jay41chen.pokedex.ui.home.HomeViewModel

@Composable
fun PokedexNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = HomeRoute, modifier = modifier.fillMaxSize()) {
        composable<HomeRoute> { backStackEntry ->
            val vm = hiltViewModel<HomeViewModel>()
            val state by vm.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                onIntent = vm::onIntent,
                onPokemonClick = { id ->
                    backStackEntry.lifecycle.runIfResumed { navController.navigate(DetailRoute(id.value)) }
                },
            )
        }
        composable<DetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<DetailRoute>()
            val vm = hiltViewModel<DetailViewModel, DetailViewModel.Factory>(
                creationCallback = { it.create(route) },
            )
            val state by vm.state.collectAsStateWithLifecycle()
            DetailScreen(
                state = state,
                onBack = { backStackEntry.lifecycle.runIfResumed { navController.popBackStack() } },
                onPokemonClick = { id ->
                    backStackEntry.lifecycle.runIfResumed { navController.navigate(DetailRoute(id.value)) }
                },
            )
        }
    }
}
