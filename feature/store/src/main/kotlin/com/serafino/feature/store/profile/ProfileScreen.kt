package com.serafino.feature.store.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.colorFromHex
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.components.SectionLabel
import com.serafino.designsystem.loyalty.GranoMark
import com.serafino.designsystem.symbolIcon
import com.serafino.domain.SellerInfo
import com.serafino.domain.entities.loyalty.LoyaltyTier

/** Pantalla del Perfil (gate de login + tarjeta de fidelidad). Espeja `ProfileView`/`ProfileContentView`. */
@Composable
fun ProfileScreen(presenter: ProfilePresenter) {
    val interactor = presenter.interactor
    val data = interactor.data
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(Theme.Spacing.md), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            when (val state = interactor.state) {
                ProfileInteractor.State.SignedOut, ProfileInteractor.State.SigningIn ->
                    SignInGate(state == ProfileInteractor.State.SigningIn, data.signInError, presenter::signIn)
                ProfileInteractor.State.Loading ->
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Theme.Palette.caramel)
                    }
                is ProfileInteractor.State.Error ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
                        EmptyStateView(Icons.Filled.WifiOff, "No pudimos cargar tu perfil", state.message)
                        CTAButton("Reintentar", icon = Icons.Filled.Refresh, onClick = presenter::retry)
                    }
                ProfileInteractor.State.Loaded -> Loaded(data, presenter)
            }
        }
    }
}

@Composable
private fun SignInGate(isSigningIn: Boolean, error: String?, onSignIn: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
        GranoMark(size = 72.dp, tint = Theme.Palette.gold)
        Text("Tu programa de granos", style = SerafinoType.title, fontWeight = FontWeight.Bold, color = Theme.Palette.foam, textAlign = TextAlign.Center)
        Text("Iniciá sesión para sumar granos en cada compra, subir de nivel y canjear café gratis y beneficios.", style = SerafinoType.subheadline, color = Theme.Palette.latte, textAlign = TextAlign.Center)

        // Teaser de beneficios (engancha antes del login, como iOS).
        GlassCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "Sumás 1 grano por cada compra",
                    "Canjeás granos por café gratis y merch",
                    "Café del Día con descuento que crece con tu nivel",
                ).forEach { perk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(16.dp))
                        Text(perk, style = SerafinoType.subheadline, color = Theme.Palette.foam)
                    }
                }
            }
        }

        if (isSigningIn) {
            CircularProgressIndicator(color = Theme.Palette.caramel)
        } else {
            CTAButton("Continuar con Google", icon = Icons.AutoMirrored.Filled.Login, tint = Theme.Palette.gold, onClick = onSignIn)
        }
        error?.let { Text(it, style = SerafinoType.caption, color = Theme.Palette.berry, textAlign = TextAlign.Center) }

        // Nota legal con enlaces (requisito de venta online; espeja iOS).
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Al iniciar sesión aceptás nuestros", style = SerafinoType.caption2, color = Theme.Palette.latte)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Términos", style = SerafinoType.caption2, fontWeight = FontWeight.SemiBold, color = Theme.Palette.gold, modifier = Modifier.clickable { open(SellerInfo.termsURL) })
                Text("y", style = SerafinoType.caption2, color = Theme.Palette.latte)
                Text("Política de privacidad", style = SerafinoType.caption2, fontWeight = FontWeight.SemiBold, color = Theme.Palette.gold, modifier = Modifier.clickable { open(SellerInfo.privacyURL) })
            }
        }
    }
}

@Composable
private fun Loaded(data: ProfileInteractor.Data, presenter: ProfilePresenter) {
    // Header
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(data.memberName, style = SerafinoType.title, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
        if (data.memberSinceText.isNotEmpty()) Text(data.memberSinceText, style = SerafinoType.caption, color = Theme.Palette.latte)
    }

    LoyaltyCard(data)

    CTAButton("Canjear granos", icon = Icons.Filled.CardGiftcard, tint = Theme.Palette.gold, onClick = presenter::openRedeem)

    TierLadder(data)

    BenefitsCard(data)

    ActivityCard(data)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(18.dp))
        Text(data.howToEarnText, style = SerafinoType.footnote, color = Theme.Palette.latte)
    }

    Row(
        Modifier.fillMaxWidth().clickable(onClick = presenter::signOut).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Theme.Palette.berry, modifier = Modifier.size(18.dp))
        Text("Cerrar sesión", style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.berry)
    }
}

@Composable
private fun LoyaltyCard(data: ProfileInteractor.Data) {
    val accent = colorFromHex(data.tier.accentHex) ?: Theme.Palette.caramel
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Theme.Radius.card))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.45f))))
            .padding(Theme.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(symbolIcon(data.tier.symbolName), contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(data.tier.name.uppercase(), style = SerafinoType.caption, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GranoMark(size = 34.dp, tint = Color.White)
            Text(data.balanceText, style = SerafinoType.largeTitle, fontWeight = FontWeight.Bold, color = Color.White)
            Text(data.pointsName, style = SerafinoType.subheadline, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(bottom = 6.dp))
        }
        // Progreso al próximo nivel.
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(percent = 50)).background(Color.Black.copy(alpha = 0.25f))) {
            Box(Modifier.fillMaxWidth(data.progress.toFloat().coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(percent = 50)).background(Color.White))
        }
        Text(data.beansToNextText, style = SerafinoType.caption, color = Color.White.copy(alpha = 0.9f))
    }
}

@Composable
private fun TierLadder(data: ProfileInteractor.Data) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Tu camino")
            // Índice del nivel actual (con fallback al desbloqueado más alto), para resaltar el próximo.
            val currentIndex = data.tiers.indexOfFirst { it.id == data.tier.id }
                .takeIf { it >= 0 }
                ?: data.tiers.indexOfLast { data.unlockedTierIDs.contains(it.id) }.coerceAtLeast(0)
            data.tiers.forEachIndexed { index, tier ->
                val unlocked = data.unlockedTierIDs.contains(tier.id)
                val isCurrent = tier.id == data.tier.id
                val isNext = index == currentIndex + 1 && !data.isMaxTier
                val accent = colorFromHex(tier.accentHex) ?: Theme.Palette.caramel
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(percent = 50)).background(if (unlocked) accent else Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                        Icon(if (unlocked) symbolIcon(tier.symbolName) else Icons.Filled.Lock, contentDescription = null, tint = if (unlocked) Color.White else Theme.Palette.latte, modifier = Modifier.size(16.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(tier.name, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = if (unlocked) Theme.Palette.foam else Theme.Palette.latte)
                        Text(
                            tier.minLabel.ifEmpty { "Desde ${tier.threshold} granos" },
                            style = SerafinoType.caption,
                            color = if (isNext) Theme.Palette.gold else Theme.Palette.latte,
                            maxLines = 1,
                        )
                        // El % del Café del Día que fija el nivel: el beneficio que define cada escalón.
                        tier.cafeDelDiaPct?.let { pct ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = if (unlocked || isNext) Theme.Palette.gold else Theme.Palette.latte, modifier = Modifier.size(11.dp))
                                Text("Café del Día $pct%", style = SerafinoType.caption2, fontWeight = FontWeight.Medium, color = if (unlocked || isNext) Theme.Palette.gold else Theme.Palette.latte)
                            }
                        }
                    }
                    // Indicador de estado por nodo: actual ("Tu nivel"), pasado (check), bloqueado (lock).
                    when {
                        isCurrent -> Text(
                            "Tu nivel",
                            style = SerafinoType.caption2,
                            fontWeight = FontWeight.Bold,
                            color = Theme.Palette.noir,
                            modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(Theme.Palette.gold).padding(horizontal = 10.dp, vertical = 5.dp),
                        )
                        unlocked -> Icon(Icons.Filled.Check, contentDescription = "Nivel alcanzado", tint = Theme.Palette.gold, modifier = Modifier.size(16.dp))
                        else -> Icon(Icons.Filled.Lock, contentDescription = "Nivel bloqueado", tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitsCard(data: ProfileInteractor.Data) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Tus beneficios")
            data.perks.forEach { perk ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(16.dp))
                    Text(perk, style = SerafinoType.subheadline, color = Theme.Palette.foam)
                }
            }
            if (data.nextPerks.isNotEmpty() && data.nextTierName != null) {
                Text("En ${data.nextTierName}:", style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte, modifier = Modifier.padding(top = 6.dp))
                data.nextPerks.forEach { perk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Theme.Palette.latte, modifier = Modifier.size(16.dp))
                        Text(perk, style = SerafinoType.subheadline, color = Theme.Palette.latte)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(data: ProfileInteractor.Data) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
            SectionLabel("Actividad")
            if (data.activity.isEmpty()) {
                Text(
                    "Todavía no tenés movimientos. Tu primera compra suma granos al instante.",
                    style = SerafinoType.footnote,
                    color = Theme.Palette.latte,
                )
            }
            data.activity.forEach { row ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(symbolIcon(row.symbol), contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.orderLabel ?: row.typeText, style = SerafinoType.subheadline, color = Theme.Palette.foam, maxLines = 1)
                        Text("${row.typeText} · ${row.dateText}", style = SerafinoType.caption, color = Theme.Palette.latte)
                    }
                    Text(row.amountText, style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = if (row.isCredit) Theme.Palette.green else Theme.Palette.berry)
                }
            }
        }
    }
}
