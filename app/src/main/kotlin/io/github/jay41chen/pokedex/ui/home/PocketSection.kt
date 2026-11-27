package io.github.jay41chen.pokedex.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.CaptureId
import io.github.jay41chen.pokedex.domain.model.CapturedPokemon
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import io.github.jay41chen.pokedex.domain.model.PokemonId
import io.github.jay41chen.pokedex.ui.component.PokemonTile
import io.github.jay41chen.pokedex.ui.component.TileRowStartPaddingDp
import io.github.jay41chen.pokedex.ui.component.TileSpacingDp

/**
 * @param scrollToNewest 使用者剛按下捕捉時為 true。新卡片插在第 0 個，但 LazyRow 依 key 停在原位，
 * 新卡會落在畫面左側外面，所以要捲回最前面。綁在捕捉動作而不是 pocket 內容：
 * 旋轉後 LaunchedEffect 會重跑、釋放第一張時第 0 個也會變，這兩種都不該捲。
 * @param onScrolledToNewest 捲完後通知呼叫端清掉旗標。
 */
@Composable
fun PocketSection(
    pocket: List<CapturedPokemon>,
    onRelease: (CaptureId) -> Unit,
    onPokemonClick: (PokemonId) -> Unit,
    tileWidth: Dp,
    scrollToNewest: Boolean,
    onScrolledToNewest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 空 Pocket 沒有 LazyRow；空 → 第一次捕捉時 LazyRow 新出現本來就在 0，捲到 0 無害並清旗標
    val listState = rememberLazyListState()
    LaunchedEffect(pocket.firstOrNull()?.captureId) {
        if (scrollToNewest && pocket.isNotEmpty()) {
            listState.animateScrollToItem(0)
            onScrolledToNewest()
        }
    }
    // 高度由卡片實際高度決定（不寫死），空或不空都一樣高，字體放大時一起變高
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SectionHeader(
            title = stringResource(R.string.pocket_title),
            count = pocket.size,
        )
        if (pocket.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // 看不見的佔位卡：讓空的 Pocket 和有卡片時一樣高（含字體放大），不寫死高度
                PokemonTile(
                    card = PocketPlaceholderCard,
                    ballContentDescription = "",
                    onBallClick = {},
                    onClick = {},
                    modifier = Modifier
                        .padding(start = TileRowStartPaddingDp.dp)
                        .align(Alignment.CenterStart)
                        .width(tileWidth)
                        .alpha(0f)
                        .clearAndSetSemantics {},
                )
                Text(text = stringResource(R.string.pocket_empty), style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val releaseText = stringResource(R.string.release_button)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = TileRowStartPaddingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(TileSpacingDp.dp),
            ) {
                items(pocket, key = { it.captureId.value }) { captured ->
                    PokemonTile(
                        card = captured.pokemon,
                        ballContentDescription = releaseText,
                        onBallClick = { onRelease(captured.captureId) },
                        onClick = { onPokemonClick(captured.pokemon.id) },
                        modifier = Modifier.animateItem().width(tileWidth),
                    )
                }
            }
        }
    }
}

// 名稱非空，確保量到一行文字高度
private val PocketPlaceholderCard = PokemonCard(PokemonId(0), "Placeholder", null)

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
