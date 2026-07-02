package com.serafino.feature.store.checkout

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.components.SectionLabel
import com.serafino.designsystem.glassCapsule
import com.serafino.domain.entities.loyalty.LoyaltyRewardType
import com.serafino.domain.entities.loyalty.LoyaltyVoucher
import com.serafino.domain.entities.store.GrindOption
import com.serafino.domain.services.CheckoutField
import com.serafino.domain.services.Money

/** Pantalla del checkout. Espeja `CheckoutView`/`CheckoutContentView` de iOS. */
@Composable
fun CheckoutScreen(presenter: CheckoutPresenter, onBack: () -> Unit) {
    val interactor = presenter.interactor
    val data = interactor.data
    val context = LocalContext.current
    LaunchedEffect(Unit) { presenter.onAppear() }

    // Pago creado → abrir Mercado Pago en el navegador. La navegación al estado del pedido se hace
    // al VOLVER del navegador (ON_RESUME), no antes, igual que el `onDismiss` del SafariView de iOS:
    // así no se muestra "estado del pedido" mientras el usuario todavía no pagó.
    var awaitingPaymentReturn by remember { mutableStateOf(false) }
    LaunchedEffect(data.payment) {
        val payment = data.payment ?: return@LaunchedEffect
        val launched = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payment.url)))
        }.isSuccess
        presenter.clearPayment()
        // Si no se pudo abrir el navegador, confirmamos igual (el pedido ya existe server-side).
        if (launched) awaitingPaymentReturn = true else presenter.didReturnFromPayment()
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingPaymentReturn) {
                awaitingPaymentReturn = false
                presenter.didReturnFromPayment()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Theme.Spacing.md, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).glassCapsule().clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Theme.Palette.foam, modifier = Modifier.size(18.dp))
                }
                Text("Pagar", style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(Theme.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
            ) {
                (state(interactor) as? CheckoutInteractor.State.Error)?.let { err ->
                    Text(err.message, style = SerafinoType.subheadline, color = Theme.Palette.berry)
                }

                ContactCard(data, presenter)
                ShippingCard(data, presenter)
                GrindCard(data, presenter)
                if (data.vouchers.isNotEmpty()) VouchersCard(data, presenter)
                PromoCard(data, presenter)
                SummaryCard(data)
            }

            Column(Modifier.fillMaxWidth().background(Theme.Palette.noirDeep.copy(alpha = 0.92f)).padding(Theme.Spacing.md)) {
                val creating = interactor.state is CheckoutInteractor.State.CreatingPreference
                if (creating) {
                    Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.Palette.caramel, modifier = Modifier.size(28.dp))
                    }
                } else {
                    CTAButton("Pagar · ${Money.ars(data.total)}", icon = Icons.Filled.CreditCard, onClick = presenter::pay)
                }
            }
        }
    }
}

private fun state(interactor: CheckoutInteractor): CheckoutInteractor.State = interactor.state

@Composable
private fun ContactCard(data: CheckoutInteractor.Data, presenter: CheckoutPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Contacto")
            Field("Nombre y apellido", data.form.name, data.fieldErrors[CheckoutField.Name]) { presenter.updateField(CheckoutField.Name, it) }
            Field("Email", data.form.email, data.fieldErrors[CheckoutField.Email], KeyboardType.Email) { presenter.updateField(CheckoutField.Email, it) }
            Field("Teléfono", data.form.phone, data.fieldErrors[CheckoutField.Phone], KeyboardType.Phone) { presenter.updateField(CheckoutField.Phone, it) }
        }
    }
}

@Composable
private fun ShippingCard(data: CheckoutInteractor.Data, presenter: CheckoutPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Envío")
            Field("Calle y número", data.form.street, data.fieldErrors[CheckoutField.Street]) { presenter.updateField(CheckoutField.Street, it) }
            Field("Localidad", data.form.city, data.fieldErrors[CheckoutField.City]) { presenter.updateField(CheckoutField.City, it) }
            Field("Provincia", data.form.province, data.fieldErrors[CheckoutField.Province]) { presenter.updateField(CheckoutField.Province, it) }
            Field("Código postal", data.form.zip, data.fieldErrors[CheckoutField.Zip], KeyboardType.Number) { presenter.updateField(CheckoutField.Zip, it) }
            if (data.freeShipping) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Theme.Palette.green, modifier = Modifier.size(16.dp))
                    Text("Envío gratis en tu zona (San Miguel)", style = SerafinoType.caption, color = Theme.Palette.green)
                }
            }
        }
    }
}

@Composable
private fun GrindCard(data: CheckoutInteractor.Data, presenter: CheckoutPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Molienda")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrindOption.entries.forEach { grind ->
                    val isOn = grind == data.grind
                    Column(
                        Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .then(if (isOn) Modifier.background(Theme.Palette.caramel) else Modifier.glassCapsule(interactive = false))
                            .clickable { presenter.selectGrind(grind) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(grind.label, style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = if (isOn) Color.White else Theme.Palette.foam)
                    }
                }
            }
            Text(data.grind.hint, style = SerafinoType.caption, color = Theme.Palette.latte)
        }
    }
}

private val voucherDayMonth by lazy { java.text.SimpleDateFormat("d MMM", java.util.Locale("es", "AR")) }

/**
 * Línea de detalle del vale: descuento/formato + vencimiento ("10% de descuento · Vence el 5 jul").
 * Espeja `detailText` de `VoucherFieldView` de iOS.
 */
private fun voucherDetail(voucher: LoyaltyVoucher): String? {
    val base = when (voucher.type) {
        LoyaltyRewardType.CafeDelDia -> voucher.pct?.let { "$it% de descuento" } ?: "Descuento del Café del Día"
        LoyaltyRewardType.FreeProduct -> voucher.format?.let { "Café gratis · $it" } ?: "Café gratis"
        else -> null
    }
    val expiry = voucher.expiresAt?.let { "Vence el ${voucherDayMonth.format(it)}" }
    return listOfNotNull(base, expiry).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun VouchersCard(data: CheckoutInteractor.Data, presenter: CheckoutPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Tus vales")
            data.vouchers.forEach { voucher ->
                val isOn = voucher.id == data.selectedVoucherID
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (isOn) Modifier.background(Theme.Palette.clayDeep.copy(alpha = 0.4f)) else Modifier)
                        .border(1.dp, if (isOn) Theme.Palette.caramel else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { presenter.selectVoucher(voucher.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(voucher.label, style = SerafinoType.subheadline, color = Theme.Palette.foam, maxLines = 1)
                        voucherDetail(voucher)?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.latte) }
                    }
                    if (isOn) Text("✓", style = SerafinoType.headline, color = Theme.Palette.caramel)
                }
            }
            data.freeCoffeeOptions.takeIf { it.isNotEmpty() }?.let { options ->
                Text("Elegí tu café gratis:", style = SerafinoType.caption, color = Theme.Palette.latte)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        val isOn = option.id == data.selectedFreeCoffeeID
                        Text(
                            option.name,
                            style = SerafinoType.caption,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOn) Color.White else Theme.Palette.foam,
                            modifier = Modifier
                                .clip(RoundedCornerShape(percent = 50))
                                .then(if (isOn) Modifier.background(Theme.Palette.caramel) else Modifier.glassCapsule(interactive = false))
                                .clickable { presenter.selectFreeCoffee(option.id) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            data.voucherWarning?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.berry) }
            data.voucherNote?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.green) }
        }
    }
}

@Composable
private fun PromoCard(data: CheckoutInteractor.Data, presenter: CheckoutPresenter) {
    if (data.promoLocked) return
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Código promocional")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) {
                    Field("Código", data.promoCode, null) { presenter.updatePromo(it) }
                }
                Text("Aplicar", style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.caramel, modifier = Modifier.clickable { presenter.validatePromo() }.padding(8.dp))
            }
            when (val promo = data.promoState) {
                is CheckoutInteractor.PromoState.Valid -> {
                    Text("Código aplicado: -${promo.percent}%", style = SerafinoType.caption, color = Theme.Palette.green)
                    if (data.promoSkipsDiscounted) {
                        // Un único descuento por producto: el código no toca lo que ya está en promo.
                        Text(
                            "No se aplica a los productos que ya están en promoción.",
                            style = SerafinoType.caption,
                            color = Theme.Palette.latte,
                        )
                    }
                }
                is CheckoutInteractor.PromoState.Invalid -> Text(promo.message, style = SerafinoType.caption, color = Theme.Palette.berry)
                CheckoutInteractor.PromoState.Validating -> Text("Validando…", style = SerafinoType.caption, color = Theme.Palette.latte)
                CheckoutInteractor.PromoState.None -> {}
            }
        }
    }
}

@Composable
private fun SummaryCard(data: CheckoutInteractor.Data) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Resumen")
            data.notice?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.latte) }
            SummaryRow("Subtotal", Money.ars(data.subtotal))
            if (data.discount > 0) SummaryRow("Descuento", "- ${Money.ars(data.discount)}", Theme.Palette.green)
            SummaryRow("Envío", if (data.freeShipping) "Gratis" else "A coordinar")
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = SerafinoType.headline, color = Theme.Palette.foam, modifier = Modifier.weight(1f))
                Text(Money.ars(data.total), style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: Color = Theme.Palette.foam) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = SerafinoType.subheadline, color = Theme.Palette.latte, modifier = Modifier.weight(1f))
        Text(value, style = SerafinoType.subheadline, color = valueColor)
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    error: String?,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
) {
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
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
