package com.serafino.feature.store.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.serafino.designsystem.AppBackground
import com.serafino.designsystem.SerafinoType
import com.serafino.designsystem.Theme
import com.serafino.designsystem.colorFromHex
import com.serafino.designsystem.components.CTAButton
import com.serafino.designsystem.components.DisclosureCard
import com.serafino.designsystem.components.EmptyStateView
import com.serafino.designsystem.components.GlassCard
import com.serafino.designsystem.components.SectionLabel
import com.serafino.designsystem.components.TagChip
import com.serafino.designsystem.glassCapsule
import com.serafino.designsystem.glassCard
import com.serafino.designsystem.store.CartButton
import com.serafino.designsystem.store.IntensityMeter
import com.serafino.designsystem.store.PriceView
import com.serafino.designsystem.store.Flag
import com.serafino.designsystem.store.ProductImage
import com.serafino.designsystem.store.QuantityStepper
import com.serafino.designsystem.store.RoastMeter
import com.serafino.designsystem.store.originFlagKeys
import com.serafino.designsystem.store.splitCountryPrefix
import com.serafino.designsystem.tv.CinematicHero
import com.serafino.domain.entities.store.Ficha
import com.serafino.domain.services.Money
import kotlin.math.roundToInt

/** Pantalla del detalle de producto. Espeja `ProductDetailView`/`ProductDetailContentView`. */
@Composable
fun ProductDetailScreen(presenter: ProductDetailPresenter, onBack: () -> Unit) {
    val interactor = presenter.interactor
    val data = interactor.data
    LaunchedEffect(Unit) { presenter.onAppear() }

    Box(Modifier.fillMaxSize().background(Theme.Palette.noir)) {
        AppBackground()
        when (interactor.state) {
            ProductDetailInteractor.State.Idle -> {}
            ProductDetailInteractor.State.NotFound ->
                Box(Modifier.fillMaxSize().padding(Theme.Spacing.lg), contentAlignment = Alignment.Center) {
                    EmptyStateView(Icons.Filled.Public, "Café no disponible", "No se pudo encontrar este café.")
                }
            ProductDetailInteractor.State.Loaded -> Loaded(data, presenter)
        }

        // Barra superior: volver + carrito.
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().padding(horizontal = Theme.Spacing.md, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(Modifier.size(40.dp).glassCapsule().clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Theme.Palette.foam, modifier = Modifier.size(18.dp))
            }
            CartButton(count = data.cartCount, onClick = presenter::openCart)
        }

        // Toast de confirmación al agregar al carrito (se autodescarta). Espeja `.toast()` de iOS.
        var showToast by remember { mutableStateOf(false) }
        LaunchedEffect(data.addedTick) {
            if (data.addedTick > 0) {
                showToast = true
                kotlinx.coroutines.delay(1700)
                showToast = false
            }
        }
        if (showToast) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .glassCapsule(interactive = false)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Theme.Palette.caramel, modifier = Modifier.size(18.dp))
                Text("Agregado al carrito", style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
            }
        }
    }
}

@Composable
private fun Loaded(data: ProductDetailInteractor.Data, presenter: ProductDetailPresenter) {
    val tint = Theme.Palette.gold
    val intensityTint = colorFromHex(data.accent.firstOrNull() ?: "") ?: Theme.Palette.caramel
    val totalPrice = data.selectedPricing.price * data.quantity

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            CinematicHero(height = 460.dp, backdrop = { ProductImage(url = data.imageURL, accent = data.accent, modifier = Modifier.fillMaxSize()) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badge = data.badge
                    if (!badge.isNullOrEmpty()) {
                        Text(badge.uppercase(), style = SerafinoType.caption2, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.background(tint, RoundedCornerShape(percent = 50)).padding(horizontal = 10.dp, vertical = 5.dp))
                    } else {
                        Text("CAFÉ DE ESPECIALIDAD", style = SerafinoType.caption2, fontWeight = FontWeight.Bold, color = Theme.Palette.gold)
                    }
                    Text(data.name, style = SerafinoType.largeTitle, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 3)
                }
            }

            Column(Modifier.padding(horizontal = Theme.Spacing.md), verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
                if (!data.isCombo) FormatPicker(data, presenter)
                if (data.origin.isNotEmpty() || data.roast.isNotEmpty() || data.intensity != null) {
                    Header(data, tint, intensityTint)
                }
                if (data.notes.isNotEmpty()) NotesSection(data.notes, tint)
                DisclosureCard("Sobre este café", Icons.Filled.Notes) {
                    Text(data.description, style = SerafinoType.subheadline, color = Theme.Palette.foam)
                }
                if (data.ficha != null) OriginSection(data, tint)
                ReviewsSection(data, tint, presenter)
            }
            Spacer(Modifier.height(80.dp))
        }

        // Barra de compra: cantidad + agregar.
        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Theme.Palette.noirDeep.copy(alpha = 0.92f)).padding(horizontal = Theme.Spacing.lg, vertical = Theme.Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.md),
        ) {
            QuantityStepper(quantity = data.quantity, onDecrement = presenter::decrementQuantity, onIncrement = presenter::incrementQuantity)
            Box(Modifier.weight(1f)) {
                CTAButton("Agregar · ${Money.ars(totalPrice)}", icon = Icons.Filled.AddShoppingCart, tint = tint, onClick = presenter::addToCart)
            }
        }
    }
}

@Composable
private fun FormatPicker(data: ProductDetailInteractor.Data, presenter: ProductDetailPresenter) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        SectionLabel("Tamaño", icon = Icons.Filled.Inventory2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data.formats.forEach { format ->
                val isSelected = format.weight == data.selectedWeight
                val cellMod = Modifier
                    .let { if (data.formats.size > 1) it.weight(1f) else it.width(116.dp) }
                    .glassCard(cornerRadius = 18.dp, tint = if (isSelected) Theme.Palette.clayDeep else null)
                    .clickable { presenter.selectFormat(format.weight) }
                    .padding(vertical = 12.dp, horizontal = 6.dp)
                Column(cellMod, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(format.label, style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White else Theme.Palette.latte)
                    Text(format.weight, style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Theme.Palette.foam)
                    Text(Money.ars(format.pricing.price), style = SerafinoType.footnote, fontWeight = FontWeight.SemiBold, color = if (isSelected) Color.White.copy(alpha = 0.9f) else Theme.Palette.latte)
                }
            }
            if (data.formats.size <= 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun Header(data: ProductDetailInteractor.Data, tint: Color, intensityTint: Color) {
    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (data.origin.isNotEmpty()) TagChip(data.origin, icon = Icons.Filled.LocationOn, tint = tint)
                if (data.roast.isNotEmpty()) TagChip(data.roast, icon = Icons.Filled.LocalFireDepartment, tint = tint)
            }
            // El nivel de tueste (RoastMeter) vive en la ficha de origen, igual que iOS; acá el
            // header solo muestra chips + intensidad + precio.
            data.intensity?.let { IntensityMeter(value = it, tint = intensityTint) }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
            PriceView(data.selectedPricing, prominent = true)
        }
    }
}

@Composable
private fun NotesSection(notes: List<String>, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.sm)) {
        SectionLabel("Perfil de sabor", icon = Icons.Filled.Star)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            notes.forEach { TagChip(it, tint = tint) }
        }
    }
}

/**
 * Ficha de origen rica: cabecera centrada con banderas → imagen con degradado de marca → specs en
 * tarjetas con ícono y bandera → RoastMeter → "carta del tostador" sobre papel crema con la historia
 * (capitular incluida) y la posdata de la descripción. Espeja `OriginSection`/`OriginSheet` de iOS;
 * acá se presenta como sección desplegable en vez de hoja modal.
 */
@Composable
private fun OriginSection(data: ProductDetailInteractor.Data, tint: Color) {
    val ficha = data.ficha ?: return
    val flagKeys = remember(data.origin) { originFlagKeys(data.origin) }
    DisclosureCard("Descubrir origen", Icons.Filled.Public, subtitle = "Región, proceso y la historia del café") {
        Column(verticalArrangement = Arrangement.spacedBy(Theme.Spacing.lg)) {
            OriginHeader(data, tint, flagKeys)
            // Imagen cuadrada con degradado de marca + foto (origin__media de la web).
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Theme.Palette.foam.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
            ) {
                ProductImage(url = data.imageURL, accent = data.accent, modifier = Modifier.fillMaxSize())
            }
            val specs = originSpecs(ficha)
            if (specs.isNotEmpty()) OriginSpecGrid(specs, tint, flagKeys)
            if (data.roast.isNotEmpty()) RoastMeter(roast = data.roast)
            OriginLetter(data)
        }
    }
}

@Composable
private fun OriginHeader(data: ProductDetailInteractor.Data, tint: Color, flagKeys: List<String>) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("FICHA DE ORIGEN", style = SerafinoType.caption2, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, color = tint)
        Text(data.name, style = SerafinoType.title, fontWeight = FontWeight.Bold, color = Theme.Palette.foam, textAlign = TextAlign.Center)
        if (data.origin.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (flagKeys.isEmpty()) {
                    Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(tint))
                } else {
                    flagKeys.forEach { Flag(country = it, width = 20.dp) }
                }
                Text(data.origin.uppercase(), style = SerafinoType.caption, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, color = Theme.Palette.foam.copy(alpha = 0.72f))
            }
        }
        if (data.notes.isNotEmpty()) {
            Text(
                data.notes.joinToString(" · "),
                style = SerafinoType.body.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 16.sp),
                color = Theme.Palette.latte,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private data class OriginSpec(val icon: ImageVector, val label: String, val value: String)

private fun originSpecs(f: Ficha): List<OriginSpec> = listOfNotNull(
    f.region?.takeIf { it.isNotBlank() }?.let { OriginSpec(Icons.Filled.LocationOn, "Región", it.trim()) },
    f.altitud?.takeIf { it.isNotBlank() }?.let { OriginSpec(Icons.Filled.Terrain, "Altitud", it.trim()) },
    f.variedad?.takeIf { it.isNotBlank() }?.let { OriginSpec(Icons.Filled.Eco, "Variedad", it.trim()) },
    f.proceso?.takeIf { it.isNotBlank() }?.let { OriginSpec(Icons.Filled.WaterDrop, "Proceso", it.trim()) },
    f.productor?.takeIf { it.isNotBlank() }?.let { OriginSpec(Icons.Filled.Groups, "Productor", it.trim()) },
    f.cosecha?.takeIf { it.isNotBlank() }?.let { OriginSpec(Icons.Filled.CalendarMonth, "Cosecha", it.trim()) },
)

@Composable
private fun OriginSpecGrid(specs: List<OriginSpec>, tint: Color, flagKeys: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        specs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { spec -> Box(Modifier.weight(1f)) { SpecCell(spec, tint, flagKeys) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SpecCell(spec: OriginSpec, tint: Color, flagKeys: List<String>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Theme.Palette.foam.copy(alpha = 0.06f))
            .border(1.dp, Theme.Palette.foam.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(spec.icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Text(spec.label.uppercase(), style = SerafinoType.caption2, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = Theme.Palette.foam.copy(alpha = 0.72f))
            // Banderas junto a "Región", igual que la ficha de la web.
            if (spec.label == "Región") flagKeys.forEach { Flag(country = it, width = 14.dp) }
        }
        val blend = if (spec.label == "Cosecha") cosechaSegments(spec.value) else null
        if (blend != null) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                blend.forEach { (key, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (key != null) Flag(country = key, width = 16.dp)
                        Text(text, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
                    }
                }
            }
        } else {
            Text(spec.value, style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = Theme.Palette.foam)
        }
    }
}

/**
 * Parte la cosecha de un blend ("Brasil: … · Colombia: …") en segmentos con su bandera. Devuelve
 * null si no hay ningún país (cosecha de un solo origen).
 */
private fun cosechaSegments(value: String): List<Pair<String?, String>>? {
    val parts = value.split("·").map { it.trim() }.filter { it.isNotEmpty() }
    val mapped = parts.map { part ->
        splitCountryPrefix(part)?.let { (key, _, rest) -> key to rest } ?: (null to part)
    }
    return if (mapped.any { it.first != null }) mapped else null
}

@Composable
private fun OriginLetter(data: ProductDetailInteractor.Data) {
    val historia = data.ficha?.historia?.trim().orEmpty()
    val hasHistoria = historia.isNotEmpty()
    val hasDesc = data.description.isNotEmpty()
    if (!hasHistoria && !hasDesc) return

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Theme.Palette.cream)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.LocalCafe, contentDescription = null, tint = Theme.Palette.caramel, modifier = Modifier.size(22.dp))
        Text("La historia detrás", style = SerafinoType.title2, fontWeight = FontWeight.Bold, color = Theme.Palette.espresso, textAlign = TextAlign.Center)
        if (hasHistoria) {
            // Historia con capitular (versal) en la primera letra, como la carta de la web.
            val story = buildAnnotatedString {
                withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontSize = 42.sp, color = Theme.Palette.clayDeep)) {
                    append(historia.first().toString())
                }
                withStyle(SpanStyle(fontFamily = FontFamily.Serif, fontSize = 16.sp, color = Theme.Palette.espresso)) {
                    append(historia.drop(1))
                }
            }
            Text(story, lineHeight = 24.sp, modifier = Modifier.fillMaxWidth())
        }
        if (hasDesc) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                    drawLine(
                        Theme.Palette.line,
                        Offset(0f, size.height / 2f),
                        Offset(size.width, size.height / 2f),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f)),
                    )
                }
                Text(
                    data.description,
                    style = SerafinoType.footnote.copy(fontStyle = FontStyle.Italic),
                    color = Theme.Palette.mocha,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun ReviewsSection(data: ProductDetailInteractor.Data, tint: Color, presenter: ProductDetailPresenter) {
    val subtitle = if (data.averageRating != null && data.reviewCount > 0) {
        val avg = "%.1f".format(data.averageRating).replace(".", ",")
        "★ $avg · ${data.reviewCount} ${if (data.reviewCount == 1) "reseña" else "reseñas"}"
    } else "Opiniones de la comunidad"

    DisclosureCard("Ver reseñas", Icons.Filled.RateReview, subtitle = subtitle) {
        when (data.reviewsState) {
            ProductDetailInteractor.ReviewsState.Loading -> Text("Cargando reseñas…", style = SerafinoType.footnote, color = Theme.Palette.latte)
            ProductDetailInteractor.ReviewsState.Failed ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No se pudieron cargar las reseñas.", style = SerafinoType.footnote, color = Theme.Palette.latte)
                    Text("Reintentar", style = SerafinoType.subheadline, fontWeight = FontWeight.SemiBold, color = tint, modifier = Modifier.clickable { presenter.retryReviews() })
                }
            else ->
                if (data.reviews.isEmpty()) {
                    Text("Todavía no hay reseñas para este café.", style = SerafinoType.footnote, color = Theme.Palette.latte)
                } else {
                    val fmt = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("es", "AR")) }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Resumen agregado: estrellas al promedio redondeado + promedio + cantidad (como iOS).
                        val avg = data.averageRating
                        if (avg != null && data.reviewCount > 0) {
                            val rounded = avg.roundToInt().coerceIn(0, 5)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("★".repeat(rounded) + "☆".repeat(5 - rounded), style = SerafinoType.subheadline, color = tint)
                                Text("%.1f".format(avg).replace(".", ","), style = SerafinoType.subheadline, fontWeight = FontWeight.Bold, color = Theme.Palette.foam)
                                Text("(${data.reviewCount})", style = SerafinoType.caption, color = Theme.Palette.latte)
                            }
                        }
                        // Tope de 6 reseñas, como iOS.
                        data.reviews.take(6).forEach { review ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Estrellas llenas + vacías hasta 5.
                                    val r = review.rating.coerceIn(0, 5)
                                    Text("★".repeat(r) + "☆".repeat(5 - r), style = SerafinoType.caption, color = tint)
                                    review.createdAt?.let { Text(fmt.format(it), style = SerafinoType.caption2, color = Theme.Palette.latte) }
                                }
                                Text(review.text, style = SerafinoType.footnote, color = Theme.Palette.foam)
                            }
                        }
                    }
                }
        }
    }
}
