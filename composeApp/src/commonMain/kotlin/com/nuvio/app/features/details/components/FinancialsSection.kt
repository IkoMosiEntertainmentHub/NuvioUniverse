package com.nuvio.app.features.details.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.features.details.*
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FinancialsSection(meta: MetaDetails, loadFinancials: suspend (MetaDetails) -> Financials = { FinancialsRepository.load(it) }) {
    var expanded by rememberSaveable(meta.id) { mutableStateOf(false) }
    var tab by rememberSaveable(meta.id) { mutableStateOf("Production budget") }
    var interval by rememberSaveable(meta.id) { mutableStateOf("Daily") }
    var data by remember(meta.id) { mutableStateOf<Financials?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    val uri = LocalUriHandler.current
    LaunchedEffect(expanded, meta.id, retry) {
        if (!expanded || data != null) return@LaunchedEffect
        loading = true; error = false
        try { data = loadFinancials(meta) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = true }
        finally { loading = false }
    }
    TextButton(onClick = { expanded = !expanded }) { BoxOfficeText(if (expanded) "Box Office ▴" else "Box Office ▾") }
    if (!expanded) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Production budget", "Box office history").forEach { label -> Chip(label, tab == label, { tab = label }) }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (error) TextButton(onClick = { retry++ }) { BoxOfficeText("Box-office data unavailable · Retry") }
        data?.let { values ->
            if (values.budget == null && values.worldwide == null && values.daily.isEmpty() && values.weekly.isEmpty() && values.weekends.isEmpty() && values.productionCosts.isEmpty()) {
                TextButton(onClick = { data = null; retry++ }) { BoxOfficeText("No box-office or budget records returned · Try again") }
            }
            if (tab == "Production budget") {
                if (meta.type in setOf("movie", "film")) {
                    val rows = listOf("Production budget" to money(values.budget), "Domestic gross" to money(values.domestic),
                        "International gross" to money(values.international), "Worldwide gross" to money(values.worldwide),
                        "Opening weekend" to money(values.opening), "Gross minus production budget" to money(values.grossMinusBudget),
                        "Production budget multiple" to (values.budgetMultiple?.let { ((it * 100).toLong() / 100.0).toString() + "×" } ?: "Unknown"))
                    rows.forEach { (label, value) -> BoxOfficeText("$label: $value", style = MaterialTheme.typography.bodySmall) }
                }
                values.productionCosts.forEach { cost ->
                    BoxOfficeText("${cost.level} · ${cost.title}: ${cost.amount} ${cost.currency}", style = MaterialTheme.typography.bodySmall)
                    BoxOfficeText("Sourced report · estimate status unknown · ${cost.date ?: "Date unknown"}", style = MaterialTheme.typography.labelSmall)
                    cost.source?.takeIf { it.startsWith("https://") }?.let { link -> TextButton(onClick = { uri.openUri(link) }) { BoxOfficeText("Budget source") } }
                }
                val episodeCosts = values.productionCosts.filter { it.level == "Episode" }
                knownProductionCostTotal(episodeCosts)?.let { (total, currency) ->
                    BoxOfficeText("Known episode subtotal: $total $currency (${episodeCosts.size} episodes; incomplete, not the series total)")
                }
                if (meta.type !in setOf("movie", "film") && values.productionCosts.isEmpty()) BoxOfficeText("Episode, season and series production costs: Unknown")
                BoxOfficeText("Budget comparisons are not net profit. Studio revenue share, marketing, distribution and other costs are unavailable. Break-even and net profit: Unknown.", style = MaterialTheme.typography.bodySmall)
                BoxOfficeText("Figures are reported source statements, not independently audited. No streaming revenue is inferred from viewership.", style = MaterialTheme.typography.labelSmall)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Daily", "Weekly", "Weekend").forEach { label -> Chip(label, interval == label, { interval = label }) }
                }
                val points = when (interval) { "Weekly" -> values.weekly; "Weekend" -> values.weekends; else -> values.daily }
                if (points.isEmpty()) BoxOfficeText("No dated theatrical grosses available for this title.") else GrossTimeline(points, interval)
            }
            BoxOfficeText("Currency: USD for theatrical figures. Historical US/Canada domestic timeline; selected international totals only. Missing values remain unknown.", style = MaterialTheme.typography.labelSmall)
            BoxOfficeText(values.source, style = MaterialTheme.typography.labelSmall)
            BoxOfficeText("Source capture: ${values.captured ?: "Not supplied"}", style = MaterialTheme.typography.labelSmall)
            TextButton(onClick = { uri.openUri(values.sourceUrl) }) { BoxOfficeText("View data source") }
        }
    }
}

@Composable
private fun GrossTimeline(points: List<GrossPoint>, interval: String) {
    var selected by remember(points) { mutableIntStateOf(0) }
    var count by remember(points) { mutableIntStateOf(10) }
    val maximum = points.maxOf { it.gross }.coerceAtLeast(1)
    val color = Color.White
    BoxOfficeText("$interval domestic gross · maximum ${money(maximum)}", style = MaterialTheme.typography.labelSmall)
    Canvas(Modifier.fillMaxWidth().height(130.dp).pointerInput(points) {
        detectTapGestures { position -> selected = ((position.x / size.width) * (points.size - 1)).toInt().coerceIn(points.indices) }
    }) {
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = size.width * index / (points.size - 1).coerceAtLeast(1)
            val y = size.height * (1 - point.gross.toFloat() / maximum)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
        val point = points[selected.coerceIn(points.indices)]
        drawCircle(color, 4.dp.toPx(), Offset(size.width * selected / (points.size - 1).coerceAtLeast(1), size.height * (1 - point.gross.toFloat() / maximum)))
    }
    BoxOfficeText("${points.first().date} — ${points.last().date}", style = MaterialTheme.typography.labelSmall)
    val selectedPoint = points[selected.coerceIn(points.indices)]
    BoxOfficeText("${selectedPoint.date}: ${money(selectedPoint.gross)} · reported cumulative ${money(selectedPoint.cumulative)}", style = MaterialTheme.typography.bodySmall)
    points.take(count).forEach { point -> BoxOfficeText("${point.date}  ${money(point.gross)}", style = MaterialTheme.typography.bodySmall) }
    if (count < points.size) TextButton(onClick = { count += 20 }) { BoxOfficeText("Show more dates") }
}

private fun money(value: Long?): String = value?.let {
    val digits = kotlin.math.abs(it).toString().reversed().chunked(3).joinToString(",").reversed()
    (if (it < 0) "−" else "") + "$" + digits
} ?: "Unknown"

@Composable
private fun BoxOfficeText(text: String, style: TextStyle = LocalTextStyle.current) {
    Text(text = text, style = style, color = Color.White)
}
