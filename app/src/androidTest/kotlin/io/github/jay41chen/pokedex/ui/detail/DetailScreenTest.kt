package io.github.jay41chen.pokedex.ui.detail

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.Species
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.ui.theme.PokedexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun str(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val clicks = mutableListOf<PokemonId>()

    private fun detail(species: Species?) = DetailUiState.Content(
        PokemonDetail(
            card = PokemonCard(PokemonId(5), "Charmeleon", "https://example.invalid/5.png"),
            types = listOf(TypeName("fire")),
            species = species,
        )
    )

    private fun show(state: DetailUiState) {
        composeRule.setContent {
            PokedexTheme { DetailScreen(state, onBack = {}, onPokemonClick = { clicks += it }) }
        }
    }

    private val notReadySpecies =
        Species(evolvesFrom = PokemonCard(PokemonId(4), "Charmander", null), description = "desc")

    @Test
    fun U1_tappingNotReadyEvolvesFromShowsSnackbarAndDoesNotNavigate() {
        composeRule.mainClock.autoAdvance = false
        show(detail(notReadySpecies))

        composeRule.onNodeWithText("Charmander").performClick()
        composeRule.mainClock.advanceTimeBy(300)

        assertTrue(clicks.isEmpty())
        composeRule.onNodeWithText(str(R.string.detail_evolves_from_loading)).assertIsDisplayed()
    }

    @Test
    fun U2_tappingNotReadyEvolvesFromTwiceShowsOnlyOneSnackbarAndNoneQueued() {
        composeRule.mainClock.autoAdvance = false
        show(detail(notReadySpecies))
        val text = str(R.string.detail_evolves_from_loading)

        composeRule.onNodeWithText("Charmander").performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithText("Charmander").performClick()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onAllNodesWithText(text).assertCountEquals(1)

        // 已經過 600ms；Short = 4000ms + 退場動畫。累計 ~5500ms、~7000ms 檢查沒有第二則排隊出現
        composeRule.mainClock.advanceTimeBy(4900)
        composeRule.onAllNodesWithText(text).assertCountEquals(0)
        composeRule.mainClock.advanceTimeBy(1500)
        composeRule.onAllNodesWithText(text).assertCountEquals(0)
        assertTrue(clicks.isEmpty())
    }

    @Test
    fun U4a_speciesNullShowsTwoLoadingPlaceholdersAndEvolvesFromTitle() {
        composeRule.mainClock.autoAdvance = false
        show(detail(null))

        // Evolves from 的占位 + 描述的占位；守住「species 還沒到時兩處都要顯示載入中」
        composeRule.onAllNodesWithText(str(R.string.detail_loading_placeholder)).assertCountEquals(2)
        composeRule.onNodeWithText(str(R.string.detail_evolves_from)).assertIsDisplayed()
    }

    @Test
    fun U4b_speciesWithoutEvolvesFromHidesWholeEvolvesFromSection() {
        composeRule.mainClock.autoAdvance = false
        show(detail(Species(evolvesFrom = null, description = "some text")))

        // 守住「species 已到但沒有前一階段：連標題都不顯示」（不能誤當成載入中）
        composeRule.onAllNodesWithText(str(R.string.detail_evolves_from)).assertCountEquals(0)
        composeRule.onAllNodesWithText(str(R.string.detail_loading_placeholder)).assertCountEquals(0)
        composeRule.onNodeWithText("some text").assertIsDisplayed()
    }

    @Test
    fun U4c_emptyDescriptionRendersNothingInsteadOfPlaceholder() {
        composeRule.mainClock.autoAdvance = false
        show(
            detail(
                Species(
                    evolvesFrom = PokemonCard(PokemonId(4), "Charmander", "https://example.invalid/4.png"),
                    description = "",
                )
            )
        )

        // 守住「species 已到但描述為空：不顯示『載入中』占位」，且 Evolves from 正常顯示
        composeRule.onAllNodesWithText(str(R.string.detail_loading_placeholder)).assertCountEquals(0)
        composeRule.onNodeWithText("Charmander").assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.detail_evolves_from)).assertIsDisplayed()
        assertEquals(emptyList<PokemonId>(), clicks)
    }
}
