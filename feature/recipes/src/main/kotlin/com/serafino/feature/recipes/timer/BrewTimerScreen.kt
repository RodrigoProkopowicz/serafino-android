package com.serafino.feature.recipes.timer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.accentOnDark
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.TagChip
import com.serafino.designsystem.glassCard
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.liquidGlass

/** Pantalla del timer de preparación. Espeja `BrewTimerView`/`BrewTimerContentView` de iOS. */
@Composable
fun BrewTimerScreen(presenter: BrewTimerPresenter) {
    val data = presenter.interactor.data
    val tint = data.method.accentOnDark
    LaunchedEffect(Unit) { presenter.onAppear() }
    DisposableEffect(Unit) { onDispose { presenter.onDisappear() } }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        Box(
            Modifier.fillMaxSize().statusBarsPadding().padding(Theme.Spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            if (data.isFinished) FinishedView(data, tint, presenter) else ActiveView(data, tint, presenter)
        }
    }
}

@Composable
private fun ActiveView(
    data: BrewTimerInteractor.Data,
    tint: Color,
    presenter: BrewTimerPresenter,
) {
    Column(
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(data.recipeName, style = SerafinoType.headline, color = Theme.Palette.latte)
            Text(data.stepCountText, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = tint)
        }

        TimerRing(
            progress = data.stepProgress.toFloat(),
            tint = tint,
            isTimed = data.isTimedStep,
            remainingText = data.remainingText,
            manualHint = data.manualHint,
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(data.stepTitle, style = SerafinoType.title3, fontWeight = FontWeight.Bold, color = Theme.Palette.foam, textAlign = TextAlign.Center)
            Text(data.stepDetail, style = SerafinoType.subheadline, color = Theme.Palette.latte, textAlign = TextAlign.Center)
            data.waterTargetText?.let { TagChip(it, tint = tint, modifier = Modifier.padding(top = 4.dp)) }
        }

        TimerControls(
            isRunning = data.isRunning,
            isTimed = data.isTimedStep,
            canGoBack = data.currentIndex > 0,
            tint = tint,
            onPrimary = presenter::togglePrimary,
            onNext = presenter::nextStep,
            onPrevious = presenter::previousStep,
        )
    }
}

@Composable
private fun TimerRing(
    progress: Float,
    tint: Color,
    isTimed: Boolean,
    remainingText: String,
    manualHint: String?,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "ringProgress")
    Box(
        Modifier.size(276.dp).liquidGlass(CircleShape).padding(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = tint.copy(alpha = 0.16f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = tint,
                startAngle = -90f, sweepAngle = 360f * animated.coerceAtLeast(0.0001f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        if (isTimed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    remainingText,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = Theme.Palette.foam,
                )
                Text("RESTANTE", style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 26.dp),
            ) {
                Icon(Icons.Filled.TouchApp, contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
                Text(manualHint ?: "Paso manual", style = SerafinoType.callout, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam, textAlign = TextAlign.Center)
                Text("TOCÁ ✓ AL TERMINAR", style = SerafinoType.caption2, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
            }
        }
    }
}

@Composable
private fun TimerControls(
    isRunning: Boolean,
    isTimed: Boolean,
    canGoBack: Boolean,
    tint: Color,
    onPrimary: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
        SecondaryButton(Icons.Filled.FastRewind, tint, enabled = canGoBack, onClick = onPrevious)
        Box(
            Modifier.size(78.dp).background(tint, CircleShape).clickable(onClick = onPrimary),
            contentAlignment = Alignment.Center,
        ) {
            val icon = if (!isTimed) Icons.Filled.Check else if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        SecondaryButton(Icons.Filled.FastForward, tint, enabled = true, onClick = onNext)
    }
}

@Composable
private fun SecondaryButton(icon: ImageVector, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(56.dp)
            .clip(CircleShape)
            .glassCapsule()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else tint.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FinishedView(
    data: BrewTimerInteractor.Data,
    tint: Color,
    presenter: BrewTimerPresenter,
) {
    Column(
        modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg),
    ) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = tint, modifier = Modifier.size(76.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("¡Preparación completa!", style = SerafinoType.title, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
            Text("Disfrutá tu ${data.recipeName}.", style = SerafinoType.subheadline, color = Theme.Palette.latte)
        }
        Spacer(Modifier.size(Theme.Spacing.sm))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CTAButton("Listo", icon = Icons.Filled.Check, tint = tint, onClick = presenter::done)
            Row(
                Modifier.clickable(onClick = presenter::restart).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Theme.Palette.latte, modifier = Modifier.size(16.dp))
                Text("Preparar de nuevo", style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.latte)
            }
        }
    }
}
