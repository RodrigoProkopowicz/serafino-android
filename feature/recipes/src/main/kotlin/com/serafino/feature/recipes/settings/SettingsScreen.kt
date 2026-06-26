package com.serafino.feature.recipes.settings

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.components.SectionLabel
import com.serafino.domain.SellerInfo
import com.serafino.domain.entities.MeasurementSystem
import com.serafino.domain.entities.TemperatureUnit

/** Pantalla de Ajustes. Espeja `SettingsView`/`SettingsContentView` de iOS. */
@Composable
fun SettingsScreen(presenter: SettingsPresenter) {
    val data = presenter.interactor.data
    val context = LocalContext.current
    LaunchedEffect(Unit) { presenter.onAppear() }

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(Theme.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
        ) {
            Text("Ajustes", style = SerafinoType.title, fontWeight = FontWeight.Bold, color = Theme.Palette.foam, modifier = Modifier.padding(start = 4.dp, top = 8.dp))

            UnitsCard(data, presenter)
            HapticsCard(data, presenter)

            if (data.storeAvailable) {
                ContactCard(::open)
                LegalCard(::open)
            }
        }
    }
}

@Composable
private fun UnitsCard(data: SettingsInteractor.Data, presenter: SettingsPresenter) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.md)) {
            SectionLabel("Unidades", icon = Icons.Filled.Straighten)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Café y agua", style = SerafinoType.subheadline, fontWeight = FontWeight.Medium, color = Theme.Palette.foam)
                Segmented(
                    options = MeasurementSystem.entries,
                    selected = data.measurementSystem,
                    label = { it.label },
                    onSelect = presenter::setMeasurement,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Temperatura del agua", style = SerafinoType.subheadline, fontWeight = FontWeight.Medium, color = Theme.Palette.foam)
                Segmented(
                    options = TemperatureUnit.entries,
                    selected = data.temperatureUnit,
                    label = { it.label },
                    onSelect = presenter::setTemperature,
                )
            }
        }
    }
}

@Composable
private fun <T> Segmented(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(
                        if (isSelected) Theme.Palette.caramel else Color.Transparent,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    style = SerafinoType.footnote,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else Theme.Palette.latte,
                )
            }
        }
    }
}

@Composable
private fun HapticsCard(data: SettingsInteractor.Data, presenter: SettingsPresenter) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Vibration, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(20.dp))
            Text("Vibración", style = SerafinoType.subheadline, fontWeight = FontWeight.Medium, color = Theme.Palette.foam, modifier = Modifier.padding(start = 10.dp).weight(1f))
            Switch(
                checked = data.hapticsEnabled,
                onCheckedChange = presenter::setHaptics,
                colors = SwitchDefaults.colors(checkedTrackColor = Theme.Palette.caramel),
            )
        }
    }
}

@Composable
private fun ContactCard(open: (String) -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Ayuda y contacto", icon = Icons.Filled.SupportAgent)
            LinkRow("WhatsApp", "${SellerInfo.whatsappDisplay} · ${SellerInfo.supportHours}", Icons.AutoMirrored.Filled.Chat, Icons.Filled.NorthEast) { open(SellerInfo.whatsappURL) }
            RowDivider()
            LinkRow("Email", SellerInfo.email, Icons.Filled.Email, Icons.Filled.NorthEast) { open(SellerInfo.emailURL) }
            RowDivider()
            LinkRow("Instagram", SellerInfo.instagramHandle, Icons.Filled.CameraAlt, Icons.Filled.NorthEast) { open(SellerInfo.instagramURL) }
        }
    }
}

@Composable
private fun LegalCard(open: (String) -> Unit) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Legales", icon = Icons.Filled.Description)
            LinkRow("Términos y condiciones", null, Icons.Filled.Description, Icons.Filled.ChevronRight) { open(SellerInfo.termsURL) }
            RowDivider()
            LinkRow("Política de privacidad", null, Icons.Filled.PrivacyTip, Icons.Filled.ChevronRight) { open(SellerInfo.privacyURL) }
            RowDivider()
            LinkRow("Botón de arrepentimiento", "Cancelá una compra dentro de los 10 días", Icons.AutoMirrored.Filled.Undo, Icons.Filled.ChevronRight) { open(SellerInfo.regretURL) }
        }
    }
}

@Composable
private fun LinkRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    trailing: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Theme.Palette.gold, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = SerafinoType.subheadline, fontWeight = FontWeight.Medium, color = Theme.Palette.foam)
            if (subtitle != null) Text(subtitle, style = SerafinoType.caption, color = Theme.Palette.latte)
        }
        Icon(trailing, contentDescription = null, tint = Theme.Palette.latte, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RowDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 36.dp).height(1.dp).background(Color.White.copy(alpha = 0.07f)))
}
