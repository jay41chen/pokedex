package io.github.jay41chen.pokedex.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil3.compose.AsyncImage
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonDetail
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.domain.model.Species
import io.github.jay41chen.pokedex.domain.model.TypeName
import io.github.jay41chen.pokedex.ui.component.PlaceholderPainter
import io.github.jay41chen.pokedex.ui.theme.PokedexTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onPokemonClick: (PokemonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notReadyText = stringResource(R.string.detail_evolves_from_loading)
    // 已在顯示就不再排隊，避免連點堆一串
    val onEvolvesFromNotReady: () -> Unit = {
        if (snackbarHostState.currentSnackbarData == null) {
            scope.launch { snackbarHostState.showSnackbar(notReadyText) }
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                            text = formatPokemonNumber(state.detail.card.id),
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
                onEvolvesFromNotReady = onEvolvesFromNotReady,
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
    onEvolvesFromNotReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = detail.card
    val species = detail.species
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 寬度 45% 對應 mock 比例；高度 40% 讓橫向時名稱與 type chips 留在第一屏；
        // 上限 240dp 避免平板過大。BoxWithConstraints 在 verticalScroll 外層，maxHeight 是有限值
        val imageSize = min(min(maxWidth * 0.45f, maxHeight * 0.4f), 240.dp)
        Column(
            modifier = Modifier
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
                    .size(imageSize)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            FlowRow(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detail.types.forEach { TypeChip(it) }
            }
            EvolvesFromSection(
                species = species,
                onPokemonClick = onPokemonClick,
                onNotReady = onEvolvesFromNotReady,
            )
            DescriptionSection(species = species)
        }
    }
}

@Composable
private fun TypeChip(type: TypeName) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = type.value,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/** 詳細頁編號：對應 mock 的 #5、#6，不補零。 */
internal fun formatPokemonNumber(id: PokemonId): String = "#${id.value}"

/** Evolves from 列點下去要去的 id；前一階段的圖還沒抓到（imageUrl == null）就沒有 detail 可看，回傳 null 代表前一階段還沒有 detail，不導頁、改顯示載入中提示。 */
internal fun evolvesFromClickTarget(evolvesFrom: PokemonCard): PokemonId? =
    if (evolvesFrom.imageUrl != null) evolvesFrom.id else null

@Composable
private fun EvolvesFromSection(
    species: Species?,
    onPokemonClick: (PokemonId) -> Unit,
    onNotReady: () -> Unit,
) {
    val evolvesFrom = species?.evolvesFrom
    // 已有 species 但沒有前一階段：整個區塊（含標題）不顯示
    if (species != null && evolvesFrom == null) return
    val title: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.detail_evolves_from),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (evolvesFrom == null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            title()
            Text(
                text = stringResource(R.string.detail_loading_placeholder),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        val target = evolvesFromClickTarget(evolvesFrom)
        val rowModifier = Modifier.clickable {
            if (target != null) onPokemonClick(target) else onNotReady()
        }
        Row(
            modifier = rowModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                title()
                Text(text = evolvesFrom.name, style = MaterialTheme.typography.bodyLarge)
            }
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
    species = Species(evolvesFrom = Charmander, description = "When it swings\nits burning tail,\nit elevates the temperature to\nunbearably high\nlevels."),
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
    PokedexTheme { DetailScreen(DetailUiState.Loading, onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailSpeciesNullPreview() {
    PokedexTheme { DetailScreen(DetailUiState.Content(BulbasaurLoading), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailEvolvesFromClickablePreview() {
    PokedexTheme { DetailScreen(DetailUiState.Content(CharmeleonClickable), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailEvolvesFromNotClickablePreview() {
    PokedexTheme { DetailScreen(DetailUiState.Content(CharmeleonNotClickable), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailNoEvolutionEmptyDescriptionPreview() {
    PokedexTheme { DetailScreen(DetailUiState.Content(PikachuNoEvolution), onBack = {}, onPokemonClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailNoImagePreview() {
    PokedexTheme { DetailScreen(DetailUiState.Content(NoImage), onBack = {}, onPokemonClick = {}) }
}
