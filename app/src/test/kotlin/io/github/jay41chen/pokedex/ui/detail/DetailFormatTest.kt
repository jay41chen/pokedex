package io.github.jay41chen.pokedex.ui.detail

import io.github.jay41chen.pokedex.domain.model.PokemonId
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailFormatTest {
    @Test
    fun `formatPokemonNumber does not pad with zeros`() {
        assertEquals("#5", formatPokemonNumber(PokemonId(5)))
        assertEquals("#151", formatPokemonNumber(PokemonId(151)))
    }
}
