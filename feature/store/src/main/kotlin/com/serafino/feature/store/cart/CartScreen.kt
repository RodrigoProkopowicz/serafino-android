package com.serafino.feature.store.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.store.ProductImage
import com.serafino.designsystem.store.QuantityStepper

/** Pantalla del carrito. Espeja `CartView`/`CartContentView` de iOS. */
@Composable
fun CartScreen(presenter: CartPresenter, onBack: () -> Unit) {
    val data = presenter.interactor.data
    val state = presenter.interactor.state
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            TopBar("Carrito", onBack)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state) {
                    CartInteractor.State.Empty ->
                        Column(
                            Modifier.fillMaxSize().padding(Theme.Spacing.lg),
                            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmptyStateView(Icons.Filled.ShoppingBag, "Tu carrito está vacío", "Descubrí nuestros cafés de especialidad y armá tu pedido.")
                            CTAButton("Explorar la tienda", icon = Icons.Filled.ShoppingBag, onClick = presenter::explore)
                        }
                    CartInteractor.State.Loaded ->
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(Theme.Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
                        ) {
                            items(data.lines, key = { it.id }) { line ->
                                CartLineRow(line, { presenter.increment(line.id) }, { presenter.decrement(line.id) }, { presenter.remove(line.id) })
                            }
                        }
                }
            }
            if (state == CartInteractor.State.Loaded) {
                Column(
                    Modifier.fillMaxWidth().background(Theme.Palette.noirDeep.copy(alpha = 0.9f)).padding(Theme.Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subtotal", style = SerafinoType.subheadline, color = Theme.Palette.latte)
                        Box(Modifier.weight(1f))
                        Text(data.subtotalText, style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
                    }
                    CTAButton("Ir a pagar", icon = Icons.Filled.CreditCard, onClick = presenter::checkout)
                }
            }
        }
    }
}

@Composable
internal fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.md, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).glassCapsule().clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Theme.Palette.foam, modifier = Modifier.size(18.dp))
        }
        Text(title, style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
    }
}

@Composable
private fun CartLineRow(
    line: CartInteractor.LineModel,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            ProductImage(url = line.imageURL, accent = emptyList(), modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(line.name, style = SerafinoType.headline, color = Theme.Palette.foam, maxLines = 1)
                Text(line.formatText, style = SerafinoType.caption, color = Theme.Palette.latte)
                Text(line.lineTotalText, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuantityStepper(quantity = line.quantity, onDecrement = onDecrement, onIncrement = onIncrement)
                Box(Modifier.size(28.dp).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Delete, contentDescription = "Quitar del carrito", tint = Theme.Palette.latte, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
