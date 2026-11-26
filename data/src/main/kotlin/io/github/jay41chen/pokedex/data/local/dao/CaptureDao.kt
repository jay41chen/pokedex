package io.github.jay41chen.pokedex.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.jay41chen.pokedex.data.local.entity.CaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CaptureDao {
    @Upsert
    suspend fun upsert(capture: CaptureEntity)

    @Query("DELETE FROM capture WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        "SELECT c.id AS capture_id, c.captured_at, p.id AS pokemon_id, p.name, d.image_url " +
            "FROM capture c JOIN pokemon p ON p.id = c.pokemon_id " +
            "LEFT JOIN pokemon_detail d ON d.pokemon_id = p.id " +
            "ORDER BY c.captured_at DESC, c.rowid DESC",
    )
    fun observeCaptures(): Flow<List<CaptureRow>>
}
