package io.github.jay41chen.pokedex.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.usecase.groupByType
import io.github.jay41chen.pokedex.ui.theme.PokedexTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun str(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private val intents = mutableListOf<HomeIntent>()
    private val clicks = mutableListOf<PokemonId>()

    private val pikachu = PokemonCard(PokemonId(25), "Pikachu", "https://example.invalid/25.png")

    private fun state(message: UserMessage?) = HomeUiState(
        pocket = emptyList(),
        sections = groupByType(listOf(CatalogEntry(pikachu, listOf(TypeName("electric"))))),
        catalog = CatalogStatus.Ready,
        message = message,
    )

    private fun show(state: HomeUiState) {
        composeRule.setContent {
            PokedexTheme { HomeScreen(state, onIntent = { intents += it }, onPokemonClick = { clicks += it }) }
        }
    }

    @Test
    fun U3_messageShownReportedOnceOnlyAfterSnackbarFinishes() {
        composeRule.mainClock.autoAdvance = false
        show(state(UserMessage.Offline))

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithText(str(R.string.message_offline)).assertIsDisplayed()
        assertFalse(intents.contains(HomeIntent.MessageShown))

        composeRule.mainClock.advanceTimeBy(5000)
        assertEquals(1, intents.count { it == HomeIntent.MessageShown })
        assertEquals(1, intents.size)
    }

    @Test
    fun U5_tappingCatalogBallCapturesAndDoesNotOpenDetail() {
        composeRule.mainClock.autoAdvance = false
        show(state(null))

        composeRule.onNodeWithContentDescription(str(R.string.capture_button)).performClick()

        assertEquals(listOf<HomeIntent>(HomeIntent.Capture(PokemonId(25))), intents)
        assertTrue(clicks.isEmpty())
    }
}
