package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.details.*
import kotlinx.coroutines.CancellationException

@Composable
internal fun FinancialsSection(meta: MetaDetails, loadFinancials: suspend (MetaDetails) -> Financials = { FinancialsRepository.load(it) }) {
    var expanded by rememberSaveable(meta.id) { mutableStateOf(false) }
    var data by remember(meta.id) { mutableStateOf<Financials?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(expanded, meta.id, retry) {
        if (!expanded || data != null) return@LaunchedEffect
        loading = true; error = false
        try { data = loadFinancials(meta) }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { error = true }
        finally { loading = false }
    }
    TextButton(onClick = { expanded = !expanded }) {
        Text(if (expanded) "Box Office ▴" else "Box Office ▾", color = Color.White)
    }
    if (!expanded) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color.White)
        if (error) TextButton(onClick = { retry++ }) { Text("Production cost unavailable · Retry", color = Color.White) }
        data?.let { values ->
            if (meta.type in setOf("movie", "film")) {
                Text("Production cost: ${compactProductionCost(values.budget?.toDouble())}", color = Color.White)
            }
            values.productionCosts.forEach { cost ->
                Text("${cost.level} · ${cost.title}: ${compactProductionCost(cost.amount, cost.currency)}", color = Color.White,
                    style = MaterialTheme.typography.bodySmall)
            }
            knownProductionCostTotal(values.productionCosts.filter { it.level == "Episode" })?.let { (total, currency) ->
                Text("Known episode subtotal: ${compactProductionCost(total, currency)} (incomplete)", color = Color.White)
            }
            if (meta.type !in setOf("movie", "film") && values.productionCosts.isEmpty()) {
                Text("Production cost: Unknown", color = Color.White)
            }
        }
    }
}
