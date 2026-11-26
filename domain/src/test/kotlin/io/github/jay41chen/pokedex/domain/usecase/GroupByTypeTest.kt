package io.github.jay41chen.pokedex.domain.usecase

import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.model.TypeSection
import io.github.jay41chen.pokedex.domain.repository.PokemonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupByTypeTest {

    private fun card(id: Int, name: String) = PokemonCard(PokemonId(id), name, null)
    private fun entry(id: Int, name: String, vararg types: String) =
        CatalogEntry(card(id, name), types.map(::TypeName))

    private val bulbasaur = entry(1, "bulbasaur", "grass", "poison")
    private val charmander = entry(4, "charmander", "fire")
    private val charizard = entry(6, "charizard", "fire", "flying")
    private val pidgey = entry(16, "pidgey", "normal", "flying")

    private val expected = listOf(
        TypeSection(TypeName("fire"), listOf(charmander.card, charizard.card)),
        TypeSection(TypeName("flying"), listOf(charizard.card, pidgey.card)),
        TypeSection(TypeName("grass"), listOf(bulbasaur.card)),
        TypeSection(TypeName("normal"), listOf(pidgey.card)),
        TypeSection(TypeName("poison"), listOf(bulbasaur.card)),
    )

    @Test
    fun charizardAppearsInFireAndFlying_sortedByTypeThenId() {
        val result = groupByType(listOf(charizard, bulbasaur, charmander, pidgey))

        assertEquals(expected, result)
        assertEquals(
            listOf("fire", "flying", "grass", "normal", "poison"),
            result.map { it.type.value },
        )
        assertEquals(listOf(4, 6), result[0].pokemon.map { it.id.value })
        assertEquals(listOf(6, 16), result[1].pokemon.map { it.id.value })
    }

    @Test
    fun inputOrderDoesNotMatter() {
        val result = groupByType(listOf(pidgey, charizard, charmander, bulbasaur))

        assertEquals(expected, result)
    }

    @Test
    fun emptyInputReturnsEmptyList() {
        assertEquals(emptyList<TypeSection>(), groupByType(emptyList()))
    }

    @Test
    fun duplicateTypesInOneEntryAppearOnce() {
        val result = groupByType(listOf(entry(6, "charizard", "fire", "fire", "flying")))

        assertEquals(
            listOf(
                TypeSection(TypeName("fire"), listOf(card(6, "charizard"))),
                TypeSection(TypeName("flying"), listOf(card(6, "charizard"))),
            ),
            result,
        )
    }

    @Test
    fun observeTypeSectionsGroupsRepositoryCatalog() = runTest {
        val repo = object : PokemonRepository {
            override fun observeCatalog(): Flow<List<CatalogEntry>> =
                flowOf(listOf(charizard, bulbasaur, charmander, pidgey))

            override fun observeDetail(id: PokemonId): Flow<PokemonDetail?> = flowOf(null)
        }

        assertEquals(expected, ObserveTypeSections(repo)().first())
    }
}
