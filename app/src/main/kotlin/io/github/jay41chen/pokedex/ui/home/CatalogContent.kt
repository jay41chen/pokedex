package io.github.jay41chen.pokedex.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.TypeSection
import io.github.jay41chen.pokedex.ui.component.PokemonTile

@Composable
fun CatalogContent(
    catalog: CatalogStatus,
    sections: List<TypeSection>,
    onCapture: (PokemonId) -> Unit,
    onPokemonClick: (PokemonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (catalog) {
        CatalogStatus.Loading -> Centered(modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.catalog_loading),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        is CatalogStatus.Empty -> Centered(modifier) {
            Text(
                text = stringResource(
                    if (catalog.offline) R.string.catalog_empty_offline else R.string.catalog_empty_error
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        CatalogStatus.Ready -> {
            val captureText = stringResource(R.string.capture_button)
            LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
                items(sections, key = { it.type.value }) { section ->
                    Column(modifier = Modifier.animateItem().padding(vertical = 8.dp)) {
                        SectionHeader(
                            title = section.type.value.replaceFirstChar { it.titlecase() },
                            count = section.pokemon.size,
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(section.pokemon, key = { it.id.value }) { card ->
                                PokemonTile(
                                    card = card,
                                    ballContentDescription = captureText,
                                    onBallClick = { onCapture(card.id) },
                                    onClick = { onPokemonClick(card.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
