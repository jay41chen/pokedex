package io.github.jay41chen.pokedex.domain.repository

import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import kotlinx.coroutines.flow.Flow

interface PokemonRepository {
    fun observeCatalog(): Flow<List<CatalogEntry>>          // 有 detail 的寶可夢，不分組、不排序
    fun observeDetail(id: PokemonId): Flow<PokemonDetail?>  // null = 這隻的 detail 還沒抓到
}
