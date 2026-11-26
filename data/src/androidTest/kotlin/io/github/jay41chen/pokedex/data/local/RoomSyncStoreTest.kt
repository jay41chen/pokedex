package io.github.jay41chen.pokedex.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jay41chen.pokedex.data.local.entity.CaptureEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonDetailEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonSpeciesEntity
import io.github.jay41chen.pokedex.data.local.entity.PokemonTypeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSyncStoreTest {
    private lateinit var db: PokedexDatabase
    private lateinit var store: RoomSyncStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PokedexDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomSyncStore(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun pokemon(id: Int, name: String = "p$id") = PokemonEntity(id, name)

    @Test
    fun b1_pendingIds_listsMissingDetailAndSpecies() = runTest {
        store.writeList((1..20).map { pokemon(it) })
        for (id in 1..11) {
            store.writeDetail(
                PokemonDetailEntity(id, "url$id"),
                listOf(PokemonTypeEntity(id, 1, "normal")),
            )
        }

        val work = store.pendingIds()

        assertFalse(work.needList)
        assertEquals((12..20).toList(), work.detailIds)
        assertEquals((1..20).toList(), work.speciesIds)
    }

    @Test
    fun b1_pendingIds_emptyDb_needsList() = runTest {
        val work = store.pendingIds()

        assertTrue(work.needList)
        assertTrue(work.detailIds.isEmpty())
        assertTrue(work.speciesIds.isEmpty())
    }

    @Test
    fun b2_writeDetail_typeFkViolation_rollsBackDetail() = runTest {
        store.writeList((1..5).map { pokemon(it) })

        try {
            store.writeDetail(
                PokemonDetailEntity(1, "url1"),
                listOf(PokemonTypeEntity(999, 1, "normal")),
            )
            fail("expected exception")
        } catch (e: SQLiteConstraintException) {
            // expected: FK violation
        }

        assertTrue(1 in store.pendingIds().detailIds)
    }

    @Test
    fun b3_writeList_reupsertKeepsCapture() = runTest {
        store.writeList(listOf(pokemon(1, "bulbasaur"), pokemon(25, "pikachu")))
        db.captureDao().upsert(CaptureEntity("c1", 25, 1000L))

        store.writeList(listOf(pokemon(1, "bulbasaur"), pokemon(25, "pikachu")))

        val rows = db.captureDao().observeCaptures().first()
        assertEquals(listOf("c1"), rows.map { it.captureId })
    }

    @Test
    fun b4_observeCaptures_sameTimestamp_laterInsertFirst() = runTest {
        store.writeList(listOf(pokemon(1, "bulbasaur"), pokemon(25, "pikachu")))
        // Later insert "A" deliberately has the smaller id string, so rowid order and id order differ.
        db.captureDao().upsert(CaptureEntity("B", 1, 1000L))
        db.captureDao().upsert(CaptureEntity("A", 25, 1000L))

        val rows = db.captureDao().observeCaptures().first()

        assertEquals(listOf("A", "B"), rows.map { it.captureId })
    }

    @Test
    fun b5_deleteById_twice_isIdempotent() = runTest {
        store.writeList(listOf(pokemon(25, "pikachu")))
        db.captureDao().upsert(CaptureEntity("c1", 25, 1000L))

        db.captureDao().deleteById("c1")
        db.captureDao().deleteById("c1")

        assertTrue(db.captureDao().observeCaptures().first().isEmpty())
    }
}
