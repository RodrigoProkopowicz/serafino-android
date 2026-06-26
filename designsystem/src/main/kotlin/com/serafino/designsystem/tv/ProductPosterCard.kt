package com.serafino.designsystem.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.store.PriceView
import com.serafino.designsystem.store.ProductCardModel
import com.serafino.designsystem.store.ProductImage

/**
 * Poster de producto para los rieles de la Tienda: imagen vertical tipo portada, con badge, y
 * nombre / origen / precio debajo. Espeja `ProductPosterCard` de iOS.
 */
@Composable
fun ProductPosterCard(
    model: ProductCardModel,
    modifier: Modifier = Modifier,
    width: Dp = 158.dp,
) {
    val shape = RoundedCornerShape(Theme.Radius.poster)
    Column(modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box {
            ProductImage(
                url = model.imageURL,
                accent = model.accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / 1.32f)
                    .clip(shape)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
            )
            val badge = model.badge
            if (!badge.isNullOrEmpty()) {
                Text(
                    badge.uppercase(),
                    style = SerafinoType.caption2,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Theme.Palette.caramel)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(model.name, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1)
            Text(model.subtitle, style = SerafinoType.caption2, color = Color.White.copy(alpha = 0.5f), maxLines = 1)
            PriceView(model.pricing)
        }
    }
}
