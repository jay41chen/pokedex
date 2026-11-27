package io.github.jay41chen.pokedex.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import kotlinx.coroutines.flow.Flow

internal data class CatalogRow(
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @Relation(parentColumn = "id", entityColumn = "pokemon_id")
    val types: List<PokemonTypeEntity>,
)

internal data class DetailRow(
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "species_pokemon_id") val speciesPokemonId: Int?, // null = no species row
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "evolves_from_id") val evolvesFromId: Int?,
    @ColumnInfo(name = "evo_name") val evoName: String?,
    @ColumnInfo(name = "evo_image_url") val evoImageUrl: String?,
    @Relation(parentColumn = "id", entityColumn = "pokemon_id")
    val types: List<PokemonTypeEntity>,
)

@Dao
internal interface PokemonDao {
    // ORDER BY is not required by the contract (ObserveTypeSections sorts); kept for stable output. [待確認]
    @Transaction
    @Query(
        "SELECT p.id AS id, p.name AS name, d.image_url AS image_url " +
            "FROM pokemon p JOIN pokemon_detail d ON d.pokemon_id = p.id " +
            "ORDER BY p.id",
    )
    fun observeCatalog(): Flow<List<CatalogRow>>

    @Transaction
    @Query(
        "SELECT p.id AS id, p.name AS name, d.image_url AS image_url, " +
            "s.pokemon_id AS species_pokemon_id, s.description AS description, " +
            "s.evolves_from_id AS evolves_from_id, e.name AS evo_name, ed.image_url AS evo_image_url " +
            "FROM pokemon p JOIN pokemon_detail d ON d.pokemon_id = p.id " +
            "LEFT JOIN pokemon_species s ON s.pokemon_id = p.id " +
            "LEFT JOIN pokemon e ON e.id = s.evolves_from_id " +
            "LEFT JOIN pokemon_detail ed ON ed.pokemon_id = e.id " +
            "WHERE p.id = :id",
    )
    fun observeDetail(id: Int): Flow<DetailRow?>
}
