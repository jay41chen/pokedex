package io.github.jay41chen.pokedex.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data class DetailRoute(val id: Int)
