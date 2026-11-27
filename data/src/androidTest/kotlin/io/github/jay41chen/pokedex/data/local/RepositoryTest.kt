package io.github.jay41chen.pokedex.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.time.TimeSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: PokedexDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun writeDetail(id: Int, imageUrl: String = "url$id") {
        db.syncDao().writeDetail(
            PokemonDetailEntity(id, imageUrl),
            listOf(PokemonTypeEntity(id, 1, "normal")),
        )
    }

    @Test
    fun r1_catalog_onlyIncludesPokemonWithDetail() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(1, "bulbasaur"), PokemonEntity(2, "ivysaur")))
        writeDetail(1)

        val catalog = PokemonRepositoryImpl(db.pokemonDao()).observeCatalog().first()

        assertEquals(listOf(1), catalog.map { it.card.id.value })
    }

    @Test
    fun r2_detail_withoutSpecies_hasNullSpecies_andMissingDetailIsNull() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(1, "bulbasaur"), PokemonEntity(2, "ivysaur")))
        writeDetail(1)
        val repo = PokemonRepositoryImpl(db.pokemonDao())

        val detail = repo.observeDetail(PokemonId(1)).first()

        assertNotNull(detail)
        assertNull(detail!!.species)
        assertNull(repo.observeDetail(PokemonId(2)).first())
    }

    @Test
    fun r3_detail_evolvesFrom_usesPreviousStageCard() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(4, "charmander"), PokemonEntity(5, "charmeleon")))
        writeDetail(4, "url-charmander")
        writeDetail(5, "url-charmeleon")
        db.syncDao().upsertSpecies(PokemonSpeciesEntity(5, 4, "desc"))

        val detail = PokemonRepositoryImpl(db.pokemonDao()).observeDetail(PokemonId(5)).first()

        val evolvesFrom = detail!!.species!!.evolvesFrom!!
        assertEquals(4, evolvesFrom.id.value)
        assertEquals("Charmander", evolvesFrom.name)
        assertEquals("url-charmander", evolvesFrom.imageUrl)
    }

    @Test
    fun r4_captureTwice_createsTwoDistinctCaptures() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(25, "pikachu")))
        val repo = CaptureRepositoryImpl(db.captureDao(), TimeSource { 1000L })

        repo.capture(PokemonId(25))
        repo.capture(PokemonId(25))

        val captures = repo.observeCaptures().first()
        assertEquals(2, captures.size)
        assertNotEquals(captures[0].captureId, captures[1].captureId)
        assertEquals(listOf(1000L, 1000L), captures.map { it.capturedAtEpochMillis })
    }

    @Test
    fun r5_types_areOrderedBySlot_inCatalogAndDetail() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(1, "bulbasaur")))
        db.syncDao().writeDetail(
            PokemonDetailEntity(1, "url1"),
            listOf(PokemonTypeEntity(1, 2, "poison"), PokemonTypeEntity(1, 1, "grass")),
        )
        val repo = PokemonRepositoryImpl(db.pokemonDao())
        val expected = listOf("grass", "poison")

        assertEquals(expected, repo.observeCatalog().first().single().types.map(TypeName::value))
        assertEquals(expected, repo.observeDetail(PokemonId(1)).first()!!.types.map(TypeName::value))
    }

    @Test
    fun r6_detail_evolvesFrom_withoutPreviousDetail_hasNullImageUrl() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(4, "charmander"), PokemonEntity(5, "charmeleon")))
        writeDetail(5)
        db.syncDao().upsertSpecies(PokemonSpeciesEntity(5, 4, "desc"))

        val evolvesFrom = PokemonRepositoryImpl(db.pokemonDao()).observeDetail(PokemonId(5)).first()!!
            .species!!.evolvesFrom

        assertNotNull(evolvesFrom)
        assertEquals(4, evolvesFrom!!.id.value)
        assertEquals("Charmander", evolvesFrom.name)
        assertNull(evolvesFrom.imageUrl)
    }

    @Test
    fun r7_release_twice_doesNotThrow_andLeavesNoCaptures() = runTest {
        db.syncDao().upsertPokemon(listOf(PokemonEntity(1, "bulbasaur")))
        writeDetail(1)
        val repo = CaptureRepositoryImpl(db.captureDao(), TimeSource { 1000L })
        repo.capture(PokemonId(1))
        val captureId = repo.observeCaptures().first().single().captureId

        repo.release(captureId)
        repo.release(captureId)

        assertEquals(emptyList<Any>(), repo.observeCaptures().first())
    }
}
