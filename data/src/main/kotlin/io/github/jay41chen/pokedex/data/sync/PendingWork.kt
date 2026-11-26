package io.github.jay41chen.pokedex.data.sync

internal data class PendingWork(
    val needList: Boolean, // pokemon table is empty
    val detailIds: List<Int>, // in pokemon but no pokemon_detail, ascending
    val speciesIds: List<Int>, // in pokemon but no pokemon_species, ascending
)
