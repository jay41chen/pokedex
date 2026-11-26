package io.github.jay41chen.pokedex.ui.detail

import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvolvesFromClickTargetTest {
    @Test
    fun `returns the previous stage id when imageUrl is present`() {
        val card = PokemonCard(PokemonId(4), "Charmander", "https://example.com/4.png")

        assertEquals(PokemonId(4), evolvesFromClickTarget(card))
    }

    @Test
    fun `returns null when imageUrl is null`() {
        val card = PokemonCard(PokemonId(4), "Charmander", null)

        assertNull(evolvesFromClickTarget(card))
    }
}
