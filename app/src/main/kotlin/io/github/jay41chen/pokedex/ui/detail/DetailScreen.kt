package io.github.jay41chen.pokedex.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.Species
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.ui.component.PlaceholderPainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onPokemonClick: (PokemonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.detail_back),
                        )
                    }
                },
                actions = {
                    if (state is DetailUiState.Content) {
                        Text(
                            text = "#" + state.detail.card.id.value.toString().padStart(3, '0'),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (state) {
            DetailUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.catalog_loading),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            is DetailUiState.Content -> DetailContent(
                detail = state.detail,
                onPokemonClick = onPokemonClick,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    detail: PokemonDetail,
    onPokemonClick: (PokemonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = detail.card
    val species = detail.species
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AsyncImage(
            model = card.imageUrl,
            contentDescription = card.name,
            placeholder = PlaceholderPainter,
            error = PlaceholderPainter,
            fallback = PlaceholderPainter,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
        )
        Text(text = card.name, style = MaterialTheme.typography.headlineMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            detail.types.forEach { TypeChip(it) }
        }
        EvolvesFromSection(species = species, onPokemonClick = onPokemonClick)
        DescriptionSection(species = species)
    }
}

@Composable
private fun TypeChip(type: TypeName) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(
            text = type.value,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EvolvesFromSection(
    species: Species?,
    onPokemonClick: (PokemonId) -> Unit,
) {
    val evolvesFrom = species?.evolvesFrom
    // 已有 species 但沒有前一階段：整個區塊（含標題）不顯示
    if (species != null && evolvesFrom == null) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.detail_evolves_from),
            style = MaterialTheme.typography.titleSmall,
        )
        if (evolvesFrom == null) {
            Text(
                text = stringResource(R.string.detail_loading_placeholder),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            // 圖還沒抓到（imageUrl == null）的前一階段不可點，沒有 detail 可看
            val rowModifier = if (evolvesFrom.imageUrl != null) {
                Modifier.clickable { onPokemonClick(evolvesFrom.id) }
            } else {
                Modifier
            }
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = evolvesFrom.imageUrl,
                    contentDescription = evolvesFrom.name,
                    placeholder = PlaceholderPainter,
                    error = PlaceholderPainter,
                    fallback = PlaceholderPainter,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Text(text = evolvesFrom.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun DescriptionSection(species: Species?) {
    when {
        species == null -> Text(
            text = stringResource(R.string.detail_loading_placeholder),
            style = MaterialTheme.typography.bodyMedium,
        )
        species.description.isEmpty() -> Unit
        else -> Text(text = species.description, style = MaterialTheme.typography.bodyMedium)
    }
}

private val SampleImage = "https://example.invalid/1.png"

private val BulbasaurLoading = PokemonDetail(
    card = PokemonCard(PokemonId(1), "Bulbasaur", SampleImage),
    types = listOf(TypeName("grass"), TypeName("poison")),
    species = null,
)

private val Charmander = PokemonCard(PokemonId(4), "Charmander", "https://example.invalid/4.png")

private val CharmeleonClickable = PokemonDetail(
    card = PokemonCard(PokemonId(5), "Charmeleon", "https://example.invalid/5.png"),
    types = listOf(TypeName("fire")),
    species = Species(evolvesFrom = Charmander, description = "When it swings its burning tail, the temperature rises."),
)

private val CharmeleonNotClickable = CharmeleonClickable.copy(
    species = CharmeleonClickable.species?.copy(evolvesFrom = Charmander.copy(imageUrl = null)),
)

private val PikachuNoEvolution = PokemonDetail(
    card = PokemonCard(PokemonId(25), "Pikachu", "https://example.invalid/25.png"),
    types = listOf(TypeName("electric")),
    species = Species(evolvesFrom = null, description = ""),
)

private val NoImage = CharmeleonClickable.copy(
    card = CharmeleonClickable.card.copy(imageUrl = null),
)

@Preview(showBackground = true)
@Composable
private fun DetailLoadingPreview() {
    MaterialTheme { DetailScreen(DetailUiState.Loading, onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailSpeciesNullPreview() {
    MaterialTheme { DetailScreen(DetailUiState.Content(BulbasaurLoading), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailEvolvesFromClickablePreview() {
    MaterialTheme { DetailScreen(DetailUiState.Content(CharmeleonClickable), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailEvolvesFromNotClickablePreview() {
    MaterialTheme { DetailScreen(DetailUiState.Content(CharmeleonNotClickable), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailNoEvolutionEmptyDescriptionPreview() {
    MaterialTheme { DetailScreen(DetailUiState.Content(PikachuNoEvolution), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailNoImagePreview() {
    MaterialTheme { DetailScreen(DetailUiState.Content(NoImage), onBack = {}, onPokemonClick = {}) }
}
