package io.github.jay41chen.pokedex.data.local

import io.github.jay41chen.pokedex.data.local.dao.PokemonDao
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.mapper.toCard
import io.github.jay41chen.pokedex.data.mapper.toCatalogEntry
import io.github.jay41chen.pokedex.data.mapper.toDetail
import io.github.jay41chen.pokedex.data.mapper.toSpecies
import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PokemonRepositoryImpl(
    private val dao: PokemonDao,
) : PokemonRepository {

    override fun observeCatalog(): Flow<List<CatalogEntry>> =
        dao.observeCatalog().map { rows ->
            rows.map { row ->
                val card = toCard(PokemonEntity(row.id, row.name), PokemonDetailEntity(row.id, row.imageUrl))
                toCatalogEntry(card, row.types)
            }
        }

    override fun observeDetail(id: PokemonId): Flow<PokemonDetail?> =
        dao.observeDetail(id.value).map { row ->
            row ?: return@map null
            val card = toCard(PokemonEntity(row.id, row.name), PokemonDetailEntity(row.id, row.imageUrl))
            val species = row.speciesPokemonId?.let { speciesId ->
                val evolvesFromId = row.evolvesFromId
                // If the previous pokemon is not in the pokemon table, treat it as no previous stage;
                // the list is always written before species, so this should not happen in practice. [待確認]
                val evolvesFrom = if (evolvesFromId != null && row.evoName != null) {
                    toCard(
                        PokemonEntity(evolvesFromId, row.evoName),
                        row.evoImageUrl?.let { PokemonDetailEntity(evolvesFromId, it) },
                    )
                } else {
                    null
                }
                toSpecies(
                    PokemonSpeciesEntity(speciesId, evolvesFromId, row.description.orEmpty()),
                    evolvesFrom,
                )
            }
            toDetail(card, row.types, species)
        }
}
