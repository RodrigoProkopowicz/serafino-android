package com.serafino.feature.recipes.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.accentColor
import com.serafino.designsystem.accentOnDark
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.DisclosureCard
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.FavoriteHeartButton
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.components.MetricBadge
import com.serafino.designsystem.components.SectionLabel
import com.serafino.designsystem.components.TagChip
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.icon
import com.serafino.designsystem.tint
import com.serafino.designsystem.tv.CinematicHero
import com.serafino.designsystem.tv.MethodBackdrop

private fun metricIcon(id: String): ImageVector = when (id) {
    "coffee" -> Icons.Filled.Scale
    "water" -> Icons.Filled.WaterDrop
    "ratio" -> Icons.Filled.SwapHoriz
    "temp" -> Icons.Filled.Thermostat
    "time" -> Icons.Filled.Timer
    else -> Icons.Filled.Speed
}

private data class NumberHint(val icon: ImageVector, val title: String, val text: String)

private val numberHints = listOf(
    NumberHint(Icons.Filled.Scale, "Café", "Cuánto café molido usar. Conviene pesarlo: una cucharada no siempre pesa lo mismo."),
    NumberHint(Icons.Filled.WaterDrop, "Agua", "Cuánta agua lleva en total. En café, 1 mililitro pesa 1 gramo, así que podés medirla en la balanza."),
    NumberHint(Icons.Filled.SwapHoriz, "Ratio", "La proporción entre café y agua. Cuanta más agua por gramo de café, más suave la taza."),
    NumberHint(Icons.Filled.Thermostat, "Temperatura", "Qué tan caliente va el agua. Muy caliente tiende a amargar; demasiado tibia deja el café flojo."),
    NumberHint(Icons.Filled.Timer, "Tiempo", "Cuánto dura la preparación de principio a fin. Te sirve para saber si vas bien."),
    NumberHint(Icons.Filled.Speed, "Nivel", "Cuánta práctica pide. \"Fácil\" es difícil de arruinar; \"Avanzada\" pide más pulso."),
)

/** Pantalla de detalle de receta. Espeja `RecipeDetailView`/`RecipeDetailContentView` de iOS. */
@Composable
fun RecipeDetailScreen(presenter: RecipeDetailPresenter, onBack: () -> Unit) {
    val interactor = presenter.interactor
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        when (interactor.state) {
            RecipeDetailInteractor.State.Idle -> {}
            RecipeDetailInteractor.State.NotFound ->
                Box(Modifier.fillMaxSize().padding(Theme.Spacing.lg), contentAlignment = Alignment.Center) {
                    EmptyStateView(Icons.Filled.HelpOutline, "Receta no disponible", "No se pudo encontrar esta receta.")
                }
            RecipeDetailInteractor.State.Loaded ->
                LoadedDetail(interactor.data, presenter::startBrewing)
        }

        // Barra superior flotante: volver + favorito.
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Theme.Spacing.md, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                Modifier.size(40.dp).glassCapsule().clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Theme.Palette.foam, modifier = Modifier.size(18.dp))
            }
            FavoriteHeartButton(isFavorite = interactor.data.isFavorite, onClick = presenter::toggleFavorite)
        }
    }
}

@Composable
private fun LoadedDetail(data: RecipeDetailInteractor.Data, onStartBrewing: () -> Unit) {
    val tint = data.method.accentOnDark
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
        ) {
            // Hero header
            CinematicHero(height = 420.dp, backdrop = { MethodBackdrop(data.method, data.method.icon) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(data.method.displayName.uppercase(), style = SerafinoType.caption2, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                    Text(data.name, style = SerafinoType.largeTitle, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(data.summary, style = SerafinoType.subheadline, color = Color.White.copy(alpha = 0.85f))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroChip(data.difficulty.label, data.difficulty.icon, data.difficulty.tint)
                        HeroChip(data.method.shortName, data.method.icon, Color.White.copy(alpha = 0.18f))
                    }
                }
            }

            Column(
                Modifier.padding(horizontal = Theme.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
            ) {
                if (data.beginnerIntro.isNotEmpty()) IntroCard(data.beginnerIntro, tint)
                MetricsGrid(data.metrics, tint)
                NumbersGuide(tint)
                if (data.equipment.isNotEmpty()) EquipmentSection(data.equipment, tint)
                GrindCard(data.grind, tint)
                StepsSection(data.steps, tint)
                if (data.glossary.isNotEmpty()) GlossarySection(data, tint)
                if (data.flavorNotes.isNotEmpty()) FlavorSection(data.flavorNotes, tint)
                if (data.tips.isNotEmpty()) TipsSection(data.tips, tint)
            }
            Spacer(Modifier.height(if (data.hasTimedSteps) 90.dp else Theme.Spacing.lg))
        }

        if (data.hasTimedSteps) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(Theme.Spacing.md)) {
                CTAButton("Empezar a preparar", icon = Icons.Filled.PlayArrow, tint = data.method.accentColor, onClick = onStartBrewing)
            }
        }
    }
}

@Composable
private fun HeroChip(text: String, icon: ImageVector, fill: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(fill, RoundedCornerShape(percent = 50)).padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
        Text(text, style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun IntroCard(intro: String, tint: Color) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Text("Para quien recién empieza", style = SerafinoType.headline, color = Theme.Palette.foam)
            }
            Text(intro, style = SerafinoType.subheadline, color = Theme.Palette.latte)
        }
    }
}

@Composable
private fun MetricsGrid(metrics: List<RecipeDetailInteractor.Metric>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { metric ->
                    MetricBadge(metricIcon(metric.id), metric.value, metric.label, Modifier.weight(1f), tint)
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NumbersGuide(tint: Color) {
    DisclosureCard("¿Qué significan los números?", Icons.Filled.HelpOutline) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            numberHints.forEach { hint ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(hint.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(hint.title, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
                        Text(hint.text, style = SerafinoType.footnote, color = Theme.Palette.latte)
                    }
                }
            }
        }
    }
}

@Composable
private fun EquipmentSection(equipment: List<String>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        SectionLabel("Qué necesitás", icon = Icons.Filled.Circle)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                equipment.forEach { item ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Circle, contentDescription = null, tint = tint, modifier = Modifier.size(7.dp).padding(top = 6.dp))
                        Text(item, style = SerafinoType.subheadline, color = Theme.Palette.foam)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrindCard(grind: String, tint: Color) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
            Column {
                Text("MOLIENDA", style = SerafinoType.caption2, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
                Text(grind, style = SerafinoType.subheadline, color = Theme.Palette.foam)
            }
        }
    }
}

@Composable
private fun StepsSection(steps: List<RecipeDetailInteractor.StepRow>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        SectionLabel("Pasos", icon = Icons.Filled.Timer)
        steps.forEach { StepCard(it, tint) }
    }
}

@Composable
private fun StepCard(step: RecipeDetailInteractor.StepRow, tint: Color) {
    GlassCard(cornerRadius = 22.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(34.dp).background(tint, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("${step.index}", style = SerafinoType.headline, color = Color.White)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(step.title, style = SerafinoType.headline, color = Theme.Palette.foam)
                Text(step.detail, style = SerafinoType.subheadline, color = Theme.Palette.latte)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    step.durationText?.let { StepLabel(Icons.Filled.Timer, it, tint) }
                    step.waterText?.let { StepLabel(Icons.Filled.WaterDrop, it, tint) }
                    if (!step.isTimed) step.manualHint?.let { StepLabel(Icons.Filled.TouchApp, it, tint) }
                }
                step.why?.let { why ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
                        Text("Por qué — $why", style = SerafinoType.footnote, color = Theme.Palette.latte)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepLabel(icon: ImageVector, text: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
        Text(text, style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun GlossarySection(data: RecipeDetailInteractor.Data, tint: Color) {
    DisclosureCard("Palabras del café", Icons.Filled.MenuBook, subtitle = "Las que aparecen en esta receta") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            data.glossary.forEach { term ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(term.term, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = tint)
                    Text(term.definition, style = SerafinoType.footnote, color = Theme.Palette.latte)
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlavorSection(notes: List<String>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        SectionLabel("Notas de sabor", icon = Icons.Filled.Info)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            notes.forEach { TagChip(it, tint = tint) }
        }
    }
}

@Composable
private fun TipsSection(tips: List<String>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        SectionLabel("Consejos", icon = Icons.Filled.Info)
        GlassCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tips.forEach { tip ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                        Text(tip, style = SerafinoType.subheadline, color = Theme.Palette.foam)
                    }
                }
            }
        }
    }
}
