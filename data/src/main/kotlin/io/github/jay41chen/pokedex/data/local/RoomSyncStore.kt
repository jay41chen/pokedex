package io.github.jay41chen.pokedex.data.local

import androidx.room.withTransaction
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import io.github.jay41chen.pokedex.data.sync.PendingWork
import io.github.jay41chen.pokedex.data.sync.SyncStore

internal class RoomSyncStore(private val db: PokedexDatabase) : SyncStore {
    override suspend fun pendingIds(): PendingWork = db.withTransaction {
        val dao = db.syncDao()
        PendingWork(
            needList = dao.isPokemonEmpty(),
            detailIds = dao.pendingDetailIds(),
            speciesIds = dao.pendingSpeciesIds(),
        )
    }

    override suspend fun writeList(pokemon: List<PokemonEntity>) {
        db.syncDao().upsertPokemon(pokemon)
    }

    // detail 只在補缺時寫一次，所以不先刪舊 type；做每日更新（B）時要改
    override suspend fun writeDetail(detail: PokemonDetailEntity, types: List<PokemonTypeEntity>) {
        db.syncDao().writeDetail(detail, types)
    }

    override suspend fun writeSpecies(species: PokemonSpeciesEntity) {
        db.syncDao().upsertSpecies(species)
    }
}
