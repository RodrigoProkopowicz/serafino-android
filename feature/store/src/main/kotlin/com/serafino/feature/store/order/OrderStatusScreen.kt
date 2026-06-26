package com.serafino.feature.store.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.GlassCard
import com.serafino.domain.entities.store.OrderStatus

private data class StatusStyle(val icon: ImageVector, val tint: Color, val title: String, val message: String)

private fun presentation(status: OrderStatus): StatusStyle = when (status) {
    OrderStatus.Approved -> StatusStyle(Icons.Filled.Verified, Theme.Palette.green, "¡Pago aprobado!", "Gracias por tu compra. Te enviamos el comprobante por email.")
    OrderStatus.Pending, OrderStatus.InProcess -> StatusStyle(Icons.Filled.HourglassBottom, Theme.Palette.caramel, "Estamos confirmando tu pago", "Puede tardar unos minutos. Actualizá el estado o volvé más tarde.")
    OrderStatus.Rejected -> StatusStyle(Icons.Filled.Cancel, Theme.Palette.berry, "El pago no se completó", "No se pudo procesar el pago. Podés intentar de nuevo desde el carrito.")
    is OrderStatus.Unknown -> StatusStyle(Icons.Filled.HelpOutline, Theme.Palette.caramel, "Pedido recibido", "Estamos procesando tu pedido.")
}

/** Pantalla del estado del pedido. Espeja `OrderStatusView`/`OrderStatusContentView`. */
@Composable
fun OrderStatusScreen(presenter: OrderStatusPresenter) {
    val data = presenter.interactor.data
    val state = presenter.interactor.state
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir), contentAlignment = Alignment.Center) {
        AppBackground()
        when (state) {
            OrderStatusInteractor.State.Loading ->
                GlassCard(Modifier.padding(horizontal = Theme.Spacing.lg)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(20.dp)) {
                        CircularProgressIndicator(color = Theme.Palette.caramel)
                        Text("Confirmando tu pago…", style = SerafinoType.headline, color = Theme.Palette.latte)
                    }
                }
            OrderStatusInteractor.State.Failed ->
                Column(
                    Modifier.padding(horizontal = Theme.Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
                ) {
                    EmptyStateView(Icons.Filled.WifiOff, "No pudimos confirmar el pago", "Revisá tu conexión y volvé a intentar.")
                    CTAButton("Reintentar", icon = Icons.Filled.Refresh, onClick = presenter::refresh)
                }
            OrderStatusInteractor.State.Loaded -> {
                val style = presentation(data.status)
                GlassCard(Modifier.padding(horizontal = Theme.Spacing.lg)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(12.dp)) {
                        Icon(style.icon, contentDescription = null, tint = style.tint, modifier = Modifier.size(64.dp))
                        Text(style.title, style = SerafinoType.title2, fontWeight = FontWeight.Bold, color = Theme.Palette.foam, textAlign = TextAlign.Center)
                        Text(style.message, style = SerafinoType.subheadline, color = Theme.Palette.latte, textAlign = TextAlign.Center)
                        if (data.totalText.isNotEmpty()) {
                            Text(data.totalText, style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
                        }
                        if (data.status.isPending) {
                            CTAButton(if (data.isPolling) "Actualizando…" else "Actualizar estado", icon = Icons.Filled.Refresh, enabled = !data.isPolling, onClick = presenter::refresh)
                        }
                        Text("Seguir comprando", style = SerafinoType.headline, color = Theme.Palette.caramel, modifier = Modifier.clickable(onClick = presenter::continueShopping))
                    }
                }
            }
        }
    }
}
