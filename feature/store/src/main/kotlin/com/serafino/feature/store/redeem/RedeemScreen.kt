package com.serafino.feature.store.redeem

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.components.SectionLabel
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.loyalty.GranoMark
import com.serafino.designsystem.symbolIcon
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.services.CheckoutField

/** Pantalla "Canjear granos". Espeja `RedeemView`/`RedeemContentView`/`RedeemShippingSheet`. */
@Composable
fun RedeemScreen(presenter: RedeemPresenter, onBack: () -> Unit) {
    val interactor = presenter.interactor
    val data = interactor.data
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.md, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).glassCapsule().clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Theme.Palette.foam, modifier = Modifier.size(18.dp))
                }
                Text("Canjear granos", style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
            }

            when (val state = interactor.state) {
                RedeemInteractor.State.Loading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Theme.Palette.caramel) }
                is RedeemInteractor.State.Error ->
                    Column(Modifier.fillMaxSize().padding(Theme.Spacing.lg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg, Alignment.CenterVertically)) {
                        EmptyStateView(Icons.Filled.WifiOff, "No se pudo cargar", state.message)
                        CTAButton("Reintentar", icon = Icons.Filled.Refresh, onClick = presenter::retry)
                    }
                RedeemInteractor.State.Loaded -> Loaded(data, presenter)
            }
        }

        // Resultado / error como banners abajo.
        data.resultMessage?.let { Banner(it, Theme.Palette.green, presenter::clearResult, Modifier.align(Alignment.BottomCenter)) }
        data.errorMessage?.let { Banner(it, Theme.Palette.berry, presenter::clearError, Modifier.align(Alignment.BottomCenter)) }

        // Formulario de envío (overlay).
        data.shippingVoucher?.let { voucher ->
            ShippingForm(voucher, data, presenter)
        }
    }
}

@Composable
private fun Loaded(data: RedeemInteractor.Data, presenter: RedeemPresenter) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Theme.Spacing.md), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
        // Saldo.
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GranoMark(size = 30.dp, tint = Theme.Palette.gold)
            Text(data.balanceText, style = SerafinoType.largeTitle, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
            Text(data.pointsName, style = SerafinoType.subheadline, color = Theme.Palette.latte, modifier = Modifier.padding(bottom = 6.dp))
        }

        data.cafe?.let { CafeCard(it, presenter) }
        if (data.freeRewards.isNotEmpty()) RewardsSection("Café gratis", data.freeRewards, data, presenter)
        if (data.merchRewards.isNotEmpty()) RewardsSection("Merch", data.merchRewards, data, presenter)
        if (data.vouchers.isNotEmpty()) VouchersSection(data, presenter)
        Box(Modifier.height(40.dp))
    }
}

@Composable
private fun CafeCard(cafe: RedeemInteractor.CafeRow, presenter: RedeemPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Café del Día · ${cafe.dayLabel}")
            cafe.coffeeName?.let { Text(it, style = SerafinoType.headline, color = Theme.Palette.foam) }
            Text("${cafe.discountText}${cafe.capText?.let { " · $it" } ?: ""}", style = SerafinoType.subheadline, color = Theme.Palette.latte)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cafe.costLabel, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.gold, modifier = Modifier.weight(1f))
                if (cafe.canRedeem) {
                    CTAButton("Canjear", tint = Theme.Palette.gold, onClick = { presenter.redeem(cafe.redeemId) }, modifier = Modifier.width(140.dp))
                } else {
                    cafe.statusText?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.berry) }
                }
            }
        }
    }
}

@Composable
private fun RewardsSection(title: String, rewards: List<RedeemInteractor.RewardRow>, data: RedeemInteractor.Data, presenter: RedeemPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel(title)
            rewards.forEach { reward ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(symbolIcon(reward.symbol), contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(reward.label, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
                        Text("${reward.sublabel} · ${reward.costLabel}", style = SerafinoType.caption, color = Theme.Palette.latte)
                    }
                    when (val availability = reward.availability) {
                        RedeemInteractor.Availability.Redeemable ->
                            if (data.redeemingID == reward.id) {
                                CircularProgressIndicator(color = Theme.Palette.caramel, modifier = Modifier.size(22.dp))
                            } else {
                                Text("Canjear", style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = Theme.Palette.gold, modifier = Modifier.clickable { presenter.redeem(reward.id) })
                            }
                        is RedeemInteractor.Availability.Locked ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = Theme.Palette.latte, modifier = Modifier.size(13.dp))
                                Text(availability.text, style = SerafinoType.caption, color = Theme.Palette.latte)
                            }
                        is RedeemInteractor.Availability.NeedMore ->
                            Text(availability.text, style = SerafinoType.caption, color = Theme.Palette.latte)
                    }
                }
            }
        }
    }
}

@Composable
private fun VouchersSection(data: RedeemInteractor.Data, presenter: RedeemPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Tus vales")
            data.vouchers.forEach { voucher ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(symbolIcon(voucher.symbol), contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(22.dp))
                    Column(Modifier.weight(1f)) {
                        Text(voucher.label, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
                        Text(voucher.detail + (voucher.expiryText?.let { " · $it" } ?: ""), style = SerafinoType.caption, color = Theme.Palette.latte)
                    }
                    if (voucher.canShip) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clickable { presenter.redeemDirect(voucher.id) }) {
                            Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(15.dp))
                            Text("Pedir", style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = Theme.Palette.gold)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("ModifierParameter")
@Composable
private fun Banner(message: String, color: Color, onDismiss: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(Theme.Spacing.md)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color, RoundedCornerShape(14.dp))
            .clickable(onClick = onDismiss)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, style = SerafinoType.subheadline, color = Theme.Palette.foam)
    }
}

@Composable
private fun ShippingForm(voucher: RedeemInteractor.VoucherRow, data: RedeemInteractor.Data, presenter: RedeemPresenter) {
    val inFlight = data.shippingID != null
    // El scrim consume los toques para que el formulario modal no deje pasar taps a la lista de
    // canje de atrás (un `clickable(enabled=false)` NO bloquea el pass-through).
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        GlassCard(Modifier.padding(Theme.Spacing.lg)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
            ) {
                SectionLabel("Pedir con envío")
                Text(voucher.label + " · envío gratis en San Miguel", style = SerafinoType.caption, color = Theme.Palette.latte)

                // Vale de café gratis con varios cafés elegibles: el usuario elige CUÁL canjear.
                if (voucher.isCoffee && voucher.coffeeOptions.isNotEmpty()) {
                    Text("Elegí tu café", style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val selected = data.shipCoffeeByVoucher[voucher.id]
                        voucher.coffeeOptions.forEach { option ->
                            val isOn = option.id == selected
                            Text(
                                option.name,
                                style = SerafinoType.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOn) Color.White else Theme.Palette.foam,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .then(if (isOn) Modifier.background(Theme.Palette.caramel) else Modifier.glassCapsule(interactive = false))
                                    .clickable { presenter.selectShipCoffee(voucher.id, option.id) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                Field("Nombre y apellido", data.shippingForm.name, data.shippingErrors[CheckoutField.Name]) { presenter.updateShippingField(CheckoutField.Name, it) }
                Field("Email", data.shippingForm.email, data.shippingErrors[CheckoutField.Email]) { presenter.updateShippingField(CheckoutField.Email, it) }
                Field("Teléfono", data.shippingForm.phone, data.shippingErrors[CheckoutField.Phone]) { presenter.updateShippingField(CheckoutField.Phone, it) }
                Field("Calle y número", data.shippingForm.street, data.shippingErrors[CheckoutField.Street]) { presenter.updateShippingField(CheckoutField.Street, it) }
                Field("Localidad", data.shippingForm.city, data.shippingErrors[CheckoutField.City]) { presenter.updateShippingField(CheckoutField.City, it) }
                Field("Provincia", data.shippingForm.province, data.shippingErrors[CheckoutField.Province]) { presenter.updateShippingField(CheckoutField.Province, it) }
                Field("Código postal", data.shippingForm.zip, data.shippingErrors[CheckoutField.Zip]) { presenter.updateShippingField(CheckoutField.Zip, it) }

                if (voucher.isCoffee) {
                    Text("Molienda", style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GrindOption.entries.forEach { grind ->
                            val isOn = grind == data.shippingGrind
                            Text(
                                grind.label,
                                style = SerafinoType.caption,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOn) Color.White else Theme.Palette.foam,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(percent = 50))
                                    .then(if (isOn) Modifier.background(Theme.Palette.caramel) else Modifier.glassCapsule(interactive = false))
                                    .clickable { presenter.selectShippingGrind(grind) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }
                }

                data.shippingSubmitError?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.berry) }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Text("Cancelar", style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte, modifier = Modifier.clickable(enabled = !inFlight) { presenter.cancelShipping() }.padding(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (inFlight) {
                            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Theme.Palette.caramel, modifier = Modifier.size(24.dp)) }
                        } else {
                            CTAButton("Pedir con envío", tint = Theme.Palette.gold, onClick = presenter::confirmShipping)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, error: String?, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, if (error != null) Theme.Palette.berry else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = SerafinoType.subheadline.copy(color = Theme.Palette.foam),
                cursorBrush = SolidColor(Theme.Palette.caramel),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(label, style = SerafinoType.subheadline, color = Theme.Palette.latte.copy(alpha = 0.6f))
                    inner()
                },
            )
        }
        error?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.berry) }
    }
}
