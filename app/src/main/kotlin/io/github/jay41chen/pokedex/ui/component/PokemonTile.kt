package io.github.jay41chen.pokedex.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.PokemonCard
import kotlin.math.floor

/**
 * 依列寬算卡片寬度（單位 dp）：至少完整顯示 3 張，最右邊固定露出半張，提示可以橫向捲動（對應 mock）。
 * 完整張數 = max(3, floor(可用寬 / (目標寬 + 間距)))，再把可用寬平分給「完整張數 + 半張」。
 */
internal fun tileWidthDp(
    rowWidthDp: Float,
    startPaddingDp: Float = TileRowStartPaddingDp,
    spacingDp: Float = TileSpacingDp,
    targetWidthDp: Float = 110f,
): Float {
    val available = rowWidthDp - startPaddingDp
    val fullTiles = maxOf(3, floor(available / (targetWidthDp + spacingDp)).toInt())
    return (available - spacingDp * fullTiles) / (fullTiles + 0.5f)
}

internal const val TileRowStartPaddingDp = 16f
internal const val TileSpacingDp = 8f

/** 呼叫端要用 [modifier] 給寬度（見 [tileWidthDp]）；卡片本身不寫死寬度。 */
@Composable
fun PokemonTile(
    card: PokemonCard,
    ballContentDescription: String,
    onBallClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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
                    .clip(RoundedCornerShape(8.dp)),
            )
            Text(
                text = card.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // 與卡片本體是兄弟節點，不在 clickable 內；自己消費點擊
        // 點擊範圍 48dp（Material 最小觸控尺寸），整塊落在卡片內；圖示仍畫在右上角
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBallClick),
            contentAlignment = Alignment.TopEnd,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pokeball),
                contentDescription = ballContentDescription,
                tint = Color.Unspecified,
                modifier = Modifier.padding(6.dp).size(20.dp),
            )
        }
    }
}
