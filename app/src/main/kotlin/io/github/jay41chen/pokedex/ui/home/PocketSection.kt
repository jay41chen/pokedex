package io.github.jay41chen.pokedex.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.ui.component.PokemonTile

@Composable
fun PocketSection(
    pocket: List<CapturedPokemon>,
    onRelease: (CaptureId) -> Unit,
    onPokemonClick: (PokemonId) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 整個區塊固定 160.dp（標題列 32.dp + 卡片區 128.dp），空或不空都一樣
    Column(modifier = modifier.fillMaxWidth().height(160.dp)) {
        SectionHeader(
            title = stringResource(R.string.pocket_title),
            count = pocket.size,
            modifier = Modifier.height(32.dp),
        )
        if (pocket.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.pocket_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            val releaseText = stringResource(R.string.release_button)
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(pocket, key = { it.captureId.value }) { captured ->
                    PokemonTile(
                        card = captured.pokemon,
                        ballContentDescription = releaseText,
                        onBallClick = { onRelease(captured.captureId) },
                        onClick = { onPokemonClick(captured.pokemon.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium)
    }
}
