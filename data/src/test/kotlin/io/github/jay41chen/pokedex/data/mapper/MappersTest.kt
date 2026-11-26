package io.github.jay41chen.pokedex.data.mapper

import io.github.jay41chen.pokedex.data.local.entity.PokemonEntity
import io.github.jay41chen.pokedex.data.remote.PokeApiJson
import io.github.jay41chen.pokedex.data.remote.dto.PokemonDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonListDto
import io.github.jay41chen.pokedex.data.remote.dto.PokemonSpeciesDto
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MappersTest {
    private fun fixture(name: String) = javaClass.getResource("/fixtures/$name")!!.readText()

    private fun species(json: String) = PokeApiJson.decodeFromString<PokemonSpeciesDto>(json)

    private fun speciesJson(vararg entries: Pair<String, String>) =
        """{"flavor_text_entries":[${
            entries.joinToString(",") { """{"flavor_text":"${it.second}","language":{"name":"${it.first}"}}""" }
        }]}"""

    @Test
    fun `M1 pikachu evolves from pichu 172 is filtered out on read`() {
        val entity = species(fixture("pokemon-species-25.json")).toEntity(25)
        assertEquals(172, entity.evolvesFromId)
        val result = toSpecies(entity, PokemonCard(PokemonId(172), "Pichu", "x"))
        assertNull(result.evolvesFrom)
    }

    @Test
    fun `M1 charmeleon evolves from charmander 4 is kept`() {
        val entity = species(fixture("pokemon-species-5.json")).toEntity(5)
        assertEquals(4, entity.evolvesFromId)
        val card = PokemonCard(PokemonId(4), "Charmander", null)
        assertEquals(card, toSpecies(entity, card).evolvesFrom)
    }

    @Test
    fun `M1 boundary 151 is kept and 152 is filtered`() {
        val entity = species(speciesJson()).toEntity(1)
        val c151 = PokemonCard(PokemonId(151), "Mew", null)
        val c152 = PokemonCard(PokemonId(152), "Chikorita", null)
        assertEquals(c151, toSpecies(entity, c151).evolvesFrom)
        assertNull(toSpecies(entity, c152).evolvesFrom)
    }

    @Test
    fun `M2 display name mapping by id`() {
        assertEquals("Nidoran♀", toCard(PokemonEntity(29, "nidoran-f"), null).name)
        assertEquals("Farfetch’d", toCard(PokemonEntity(83, "farfetchd"), null).name)
        assertEquals("Bulbasaur", toCard(PokemonEntity(1, "bulbasaur"), null).name)
        assertEquals("Nidoran♂", toCard(PokemonEntity(32, "nidoran-m"), null).name)
        assertEquals("Mr. Mime", toCard(PokemonEntity(122, "mr-mime"), null).name)
    }

    @Test
    fun `M3 no english flavor text gives empty description`() {
        val entity = species(speciesJson("ja" to "x", "fr" to "y")).toEntity(1)
        assertEquals("", entity.description)
    }

    @Test
    fun `M3 english after non-english is still picked`() {
        val entity = species(speciesJson("ja" to "x", "en" to "hello")).toEntity(1)
        assertEquals("hello", entity.description)
    }

    @Test
    fun `M3 description whitespace normalized on read`() {
        val entity = species(speciesJson("en" to "a\\fb\\nc\\r  d ")).toEntity(1)
        assertEquals("a b c d", toSpecies(entity, null).description)
    }

    @Test
    fun `M4 empty types throws InvalidDataException`() {
        val dto = PokeApiJson.decodeFromString<PokemonDto>("""{"types":[]}""")
        assertThrows(InvalidDataException::class.java) { dto.toEntities(1) }
    }

    @Test
    fun `M4 types are sorted by slot ascending`() {
        val dto = PokeApiJson.decodeFromString<PokemonDto>(
            """{"types":[{"slot":2,"type":{"name":"flying"}},{"slot":1,"type":{"name":"fire"}}]}""",
        )
        val (_, types) = dto.toEntities(6)
        assertEquals(listOf("fire", "flying"), types.map { it.typeName })
    }

    @Test
    fun `read side sorts types by slot`() {
        val (detail, types) = PokeApiJson.decodeFromString<PokemonDto>(
            """{"types":[{"slot":1,"type":{"name":"fire"}},{"slot":2,"type":{"name":"flying"}}]}""",
        ).toEntities(6)
        val card = PokemonCard(PokemonId(6), "Charizard", detail.imageUrl)
        val expected = listOf(TypeName("fire"), TypeName("flying"))
        assertEquals(expected, toCatalogEntry(card, types.reversed()).types)
        assertEquals(expected, toDetail(card, types.reversed(), null).types)
    }

    @Test
    fun `charizard fixture parses with image and types`() {
        val dto = PokeApiJson.decodeFromString<PokemonDto>(fixture("pokemon-6.json"))
        val (detail, types) = dto.toEntities(6)
        assertNotNull(detail.imageUrl)
        assertEquals(listOf("fire", "flying"), types.map { it.typeName })
    }

    @Test
    fun `missing sprites gives null image`() {
        val dto = PokeApiJson.decodeFromString<PokemonDto>(
            """{"sprites":{"other":{}},"types":[{"slot":1,"type":{"name":"fire"}}]}""",
        )
        assertNull(dto.toEntities(1).first.imageUrl)
    }

    @Test
    fun `list ids come from trailing url segment and bad url fails`() {
        val ok = PokeApiJson.decodeFromString<PokemonListDto>(
            """{"results":[{"name":"pikachu","url":"https://pokeapi.co/api/v2/pokemon/25/"}]}""",
        ).toEntities()
        assertEquals(PokemonEntity(25, "pikachu"), ok.single())
        val bad = PokeApiJson.decodeFromString<PokemonListDto>("""{"results":[{"name":"x","url":"nope/"}]}""")
        assertThrows(InvalidDataException::class.java) { bad.toEntities() }
    }

    @Test
    fun `id is taken from last url segment even with numeric segment before it`() {
        val list = PokeApiJson.decodeFromString<PokemonListDto>(
            """{"results":[{"name":"pikachu","url":"https://pokeapi.co/api/2/pokemon/25/"}]}""",
        ).toEntities()
        assertEquals(25, list.single().id)
        val dto = species(
            """{"evolves_from_species":{"url":"https://pokeapi.co/api/2/pokemon-species/4/"},"flavor_text_entries":[]}""",
        )
        assertEquals(4, dto.toEntity(5).evolvesFromId)
    }
}
