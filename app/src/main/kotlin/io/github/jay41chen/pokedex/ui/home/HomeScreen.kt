package io.github.jay41chen.pokedex.ui.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.CatalogEntry
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.domain.model.TypeSection
import io.github.jay41chen.pokedex.domain.usecase.groupByType
import io.github.jay41chen.pokedex.ui.component.tileWidthDp
import io.github.jay41chen.pokedex.ui.theme.PokedexTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    onIntent: (HomeIntent) -> Unit,
    onPokemonClick: (PokemonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val text = state.message?.let { stringResource(it.toStringRes()) }
    val currentOnIntent by rememberUpdatedState(onIntent)
    LaunchedEffect(state.message) {
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            currentOnIntent(HomeIntent.MessageShown)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.padding(padding)) {
            // 卡片寬度依可用寬度算（至少 3 張、右邊露出半張），不寫死
            val tileWidth = tileWidthDp(maxWidth.value).dp
            Column(modifier = Modifier.fillMaxSize()) {
                PocketSection(
                    pocket = state.pocket,
                    onRelease = { onIntent(HomeIntent.Release(it)) },
                    onPokemonClick = onPokemonClick,
                    tileWidth = tileWidth,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.primary)
                CatalogContent(
                    catalog = state.catalog,
                    sections = state.sections,
                    onCapture = { onIntent(HomeIntent.Capture(it)) },
                    onPokemonClick = onPokemonClick,
                    tileWidth = tileWidth,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun UserMessage.toStringRes(): Int = when (this) {
    UserMessage.Offline -> R.string.message_offline
    UserMessage.PartialFailure -> R.string.message_partial_failure
}

private val bulbasaur = PokemonCard(PokemonId(1), "Bulbasaur", "https://example.invalid/1.png")
private val charmander = PokemonCard(PokemonId(4), "Charmander", "https://example.invalid/4.png")
private val charizard = PokemonCard(PokemonId(6), "Charizard", null)
private val pikachu = PokemonCard(PokemonId(25), "Pikachu", "https://example.invalid/25.png")

private val sampleSections = groupByType(
    listOf(
        CatalogEntry(bulbasaur, listOf(TypeName("grass"), TypeName("poison"))),
        CatalogEntry(charmander, listOf(TypeName("fire"))),
        CatalogEntry(charizard, listOf(TypeName("fire"), TypeName("flying"))),
        CatalogEntry(pikachu, listOf(TypeName("electric"))),
    )
)

private val samplePocket = listOf(
    CapturedPokemon(CaptureId("a"), pikachu, 1L),
    CapturedPokemon(CaptureId("b"), charizard, 2L),
    CapturedPokemon(CaptureId("c"), pikachu, 3L),
)

private fun previewState(
    catalog: CatalogStatus,
    pocket: List<CapturedPokemon> = emptyList(),
    sections: List<TypeSection> = emptyList(),
) = HomeUiState(pocket = pocket, sections = sections, catalog = catalog, message = null)

@Preview(showBackground = true)
@Composable
private fun HomeLoadingPreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Loading), {}, {})
}

@Preview(showBackground = true)
@Composable
private fun HomeEmptyOfflinePreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Empty(offline = true)), {}, {})
}

@Preview(showBackground = true)
@Composable
private fun HomeEmptyErrorPreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Empty(offline = false)), {}, {})
}

@Preview(showBackground = true)
@Composable
private fun HomeReadyEmptyPocketPreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Ready, sections = sampleSections), {}, {})
}

@Preview(showBackground = true)
@Composable
private fun HomeReadyWithPocketPreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Ready, samplePocket, sampleSections), {}, {})
}

@Preview(showBackground = true, widthDp = 840)
@Composable
private fun HomeTabletPreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Ready, samplePocket, sampleSections), {}, {})
}

@Preview(showBackground = true, fontScale = 1.3f)
@Composable
private fun HomeLargeFontPreview() = PokedexTheme {
    HomeScreen(previewState(CatalogStatus.Ready, samplePocket, sampleSections), {}, {})
}
