package com.nuvio.app.features.details.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.features.details.*
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private enum class ProductionSearchMode(val label: String) {
    Actor("Actor"), Character("Character"), CoStars("Co-stars"), Titles("Movies / TV"), Related("Related productions")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProductionSearchButton(meta: MetaDetails, onOpenMeta: ((MetaPreview) -> Unit)?) {
    var open by rememberSaveable(meta.id) { mutableStateOf(false) }
    var query by rememberSaveable(meta.id) { mutableStateOf("") }
    TextButton(onClick = { open = true }) { Text("Search cast and productions") }
    if (!open) return
    var mode by remember { mutableStateOf(ProductionSearchMode.Actor) }
    var people by remember { mutableStateOf<List<DiscoveryPerson>>(emptyList()) }
    var results by remember { mutableStateOf<List<MetaPreview>>(emptyList()) }
    var firstPerson by remember { mutableStateOf<DiscoveryPerson?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    var job by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    fun request(block: suspend () -> Unit) {
        job?.cancel()
        job = scope.launch {
            loading = true
            message = null
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "Metadata unavailable. Check your connection and TMDB settings, then retry." }
            finally { loading = false }
        }
    }
    fun search(nextPage: Int = 1) = request {
        when (mode) {
            ProductionSearchMode.Actor, ProductionSearchMode.CoStars -> {
                people = ProductionDiscovery.people(query)
                results = emptyList()
                if (people.isEmpty()) message = "No matching cast members."
            }
            ProductionSearchMode.Character -> {
                results = ProductionDiscovery.characters(query, meta.cast)
                if (results.isEmpty()) message = "No matching credit roles in the available current-cast filmographies."
            }
            ProductionSearchMode.Titles -> {
                val found = ProductionDiscovery.titles(query, nextPage)
                results = if (nextPage == 1) found else (results + found).distinctBy { "${it.type}:${it.id}" }
                page = nextPage
                if (found.isEmpty()) message = "No more matching productions."
            }
            ProductionSearchMode.Related -> {
                val client = UniverseRepository
                client.prefetch(meta, refresh = true)
                message = "Connections are displayed below and on the title's Universe section."
            }
        }
    }
    val states by UniverseRepository.states.collectAsState()
    val settings by MetaScreenSettingsRepository.uiState.collectAsState()
    val related = if (universeEnabled(meta, settings.universeConfiguration))
        visibleUniverseConnections(states[UniverseRepository.key(meta)]?.items.orEmpty(), settings.universeConfiguration,
            com.nuvio.app.features.watchprogress.CurrentDateProvider.todayIsoDate()) else emptyList()
    val displayed = if (mode == ProductionSearchMode.Related) related.map { it.item } else results
    AlertDialog(onDismissRequest = { job?.cancel(); open = false }, title = { Text("Cast and production search") }, text = {
        Column {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProductionSearchMode.entries.forEach { option ->
                    Chip(label = option.label, selected = option == mode, onClick = {
                        job?.cancel(); loading = false; mode = option; people = emptyList(); results = emptyList(); firstPerson = null; message = null
                        if (option == ProductionSearchMode.Related) UniverseRepository.prefetch(meta)
                    })
                }
            }
            if (mode == ProductionSearchMode.Character) Text("Matches actual character credits for up to 20 identified members of this cast. This is not a global character database.")
            if (mode == ProductionSearchMode.CoStars) Text(firstPerson?.let { "Find a co-star for ${it.name}" } ?: "Search and select the first actor, then search and select the second.")
            if (mode != ProductionSearchMode.Related) {
                OutlinedTextField(query, { query = it }, label = { Text("Search") }, singleLine = true)
                TextButton(enabled = query.trim().length >= 2 && !loading, onClick = { search() }) { Text("Search") }
            }
            if (loading || mode == ProductionSearchMode.Related && states[UniverseRepository.key(meta)]?.loading == true) LinearProgressIndicator(Modifier.fillMaxWidth())
            message?.let { Text(it) }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (mode == ProductionSearchMode.Actor || mode == ProductionSearchMode.CoStars) {
                    if (query.isBlank()) items(meta.cast.filter { it.tmdbId != null }.distinctBy { it.tmdbId }) { person ->
                        TextButton(enabled = mode != ProductionSearchMode.CoStars || firstPerson?.id != person.tmdbId, onClick = {
                            if (mode == ProductionSearchMode.CoStars && firstPerson == null) firstPerson = DiscoveryPerson(person.tmdbId!!, person.name)
                            else request {
                                results = if (mode == ProductionSearchMode.CoStars) ProductionDiscovery.sharedCredits(firstPerson!!.id, person.tmdbId!!)
                                    else ProductionDiscovery.credits(person.tmdbId!!).map { it.item }.distinctBy { "${it.type}:${it.id}" }
                                if (results.isEmpty()) message = "No matching movie or TV credits."
                            }
                        }) {
                            Text("${person.name} · ${person.role.orEmpty()}")
                        }
                    }
                    items(people, key = { "person:${it.id}" }) { person ->
                        TextButton(enabled = mode != ProductionSearchMode.CoStars || firstPerson?.id != person.id, onClick = {
                            if (mode == ProductionSearchMode.CoStars && firstPerson == null) {
                                firstPerson = person; people = emptyList(); query = ""
                            } else request {
                                results = if (mode == ProductionSearchMode.CoStars) ProductionDiscovery.sharedCredits(firstPerson!!.id, person.id)
                                    else ProductionDiscovery.credits(person.id).map { it.item }.distinctBy { "${it.type}:${it.id}" }
                                people = emptyList()
                                if (results.isEmpty()) message = "No matching movie or TV credits."
                            }
                        }) { Text(person.name) }
                    }
                }
                items(displayed, key = { "title:${it.type}:${it.id}" }) { title ->
                    TextButton(enabled = onOpenMeta != null, onClick = { open = false; onOpenMeta?.invoke(title) }) {
                        Text("${title.name} · ${title.releaseInfo ?: "Date unknown"} · ${title.type}")
                    }
                }
                if (mode == ProductionSearchMode.Titles && results.isNotEmpty()) item {
                    TextButton(enabled = !loading, onClick = { search(page + 1) }) { Text("Load more") }
                }
                if (mode == ProductionSearchMode.Related && displayed.isEmpty()) item { Text("No confirmed connections available for the enabled Universe features.") }
            }
        }
    }, confirmButton = { TextButton(onClick = { job?.cancel(); open = false }) { Text("Close") } })
}
