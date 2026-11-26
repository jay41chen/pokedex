package io.github.jay41chen.pokedex.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.jay41chen.pokedex.R
import io.github.jay41chen.pokedex.domain.model.PokemonCard

@Composable
fun PokemonTile(
    card: PokemonCard,
    ballContentDescription: String,
    onBallClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.width(96.dp)) {
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF5F5F5)),
            )
            Text(
                text = card.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // 與卡片本體是兄弟節點，不在 clickable 內；IconButton 自己消費點擊
        IconButton(
            onClick = onBallClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_pokeball),
                contentDescription = ballContentDescription,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
