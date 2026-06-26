package com.serafino.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.glassCapsule

/**
 * Call-to-action primario. En iOS 26 es `.glassProminent`; acá un botón cápsula prominente con el
 * tinte de marca. Espeja `CTAButton` de iOS.
 */
@Composable
fun CTAButton(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = Theme.Palette.caramel,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(
            containerColor = tint,
            contentColor = Color.White,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(title, style = SerafinoType.headline, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Botón corazón glass con rebote al cambiar. Espeja `FavoriteHeartButton` de iOS.
 */
@Composable
fun FavoriteHeartButton(
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (isFavorite) 1.15f else 1f, label = "heartBounce")
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.size(36.dp).glassCapsule(),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
            tint = if (isFavorite) Theme.Palette.berry else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp).scale(scale),
        )
    }
}
