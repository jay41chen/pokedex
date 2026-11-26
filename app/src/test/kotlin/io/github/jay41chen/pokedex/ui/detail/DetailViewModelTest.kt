package io.github.jay41chen.pokedex.ui.detail

import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.usecase.ObservePokemonDetail
import io.github.jay41chen.pokedex.testing.FakePokemonRepository
import io.github.jay41chen.pokedex.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pokemonRepo = FakePokemonRepository()

    private fun detailOf(id: Int, name: String) = PokemonDetail(
        card = PokemonCard(PokemonId(id), name, "https://example.com/$id.png"),
        types = listOf(TypeName("fire")),
        species = null,
    )

    @Test
    fun `shows Loading until detail for the navigated id arrives, then Content`() {
        pokemonRepo.detail(PokemonId(5)).value = detailOf(5, "Charmeleon")
        val vm = DetailViewModel(PokemonId(4), ObservePokemonDetail(pokemonRepo))

        assertEquals(DetailUiState.Loading, vm.state.value)

        val charmander = detailOf(4, "Charmander")
        pokemonRepo.detail(PokemonId(4)).value = charmander

        assertEquals(DetailUiState.Content(charmander), vm.state.value)
    }

    @Test
    fun `returns to Loading when detail becomes null`() {
        val charmander = detailOf(4, "Charmander")
        pokemonRepo.detail(PokemonId(4)).value = charmander
        val vm = DetailViewModel(PokemonId(4), ObservePokemonDetail(pokemonRepo))

        assertEquals(DetailUiState.Content(charmander), vm.state.value)

        pokemonRepo.detail(PokemonId(4)).value = null

        assertEquals(DetailUiState.Loading, vm.state.value)
    }
}
