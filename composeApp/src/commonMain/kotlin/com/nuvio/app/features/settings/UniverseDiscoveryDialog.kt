package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.details.*
import com.nuvio.app.features.home.MetaPreview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun UniverseDiscoveryDialog(onDismiss: () -> Unit) {
    val options by MetaScreenSettingsRepository.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MetaPreview>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    fun search(next: Int) {
        if (loading || query.isBlank()) return
        scope.launch {
            loading = true
            message = null
            try {
                val found = ProductionDiscovery.titles(query, next)
                results = if (next == 1) found else (results + found).distinctBy { "${it.type}:${it.id}" }
                page = next
                if (found.isEmpty()) message = "No more matching movies or shows."
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "Could not load metadata. Check your connection and TMDB settings, then retry." }
            finally { loading = false }
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Discover universes") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text("Search a movie or show to manage its connected-title universe. Entries are title-rooted, not inferred shared universes. Switches affect generated browsing only.")
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Movie or TV-show title") }, singleLine = true)
                TextButton(enabled = !loading && query.isNotBlank(), onClick = { search(1) }) { Text("Search") }
                if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                message?.let { Text(it) }
                LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(options.universeConfiguration.selections, key = { "saved:${it.type}:${it.id}" }) { selection ->
                        SettingsSwitchRow(title = selection.name, description = "Connected-title universe", checked = selection.enabled, isTablet = false,
                            onCheckedChange = { MetaScreenSettingsRepository.setUniverseSelection(selection.copy(enabled = it)) })
                    }
                    items(results, key = { "found:${it.type}:${it.id}" }) { title ->
                        TextButton(onClick = {
                            scope.launch {
                                loading = true
                                try {
                                    val imdb = com.nuvio.app.features.tmdb.TmdbService.ensureTmdbId(title.id, title.type)?.toIntOrNull()
                                        ?.let { com.nuvio.app.features.tmdb.TmdbService.tmdbToImdb(it, title.type) }
                                    if (imdb == null) { message = "No IMDb connections identity available for this title." }
                                    else {
                                        MetaScreenSettingsRepository.setUniverseSelection(UniverseSelection(imdb, title.type, title.name, tmdbId = title.id))
                                        UniverseRepository.prefetch(MetaDetails(imdb, title.type, title.name))
                                        message = "Added ${title.name}. Its connections appear on that title's Details Screen when available."
                                    }
                                } catch (e: CancellationException) { throw e }
                                catch (_: Exception) { message = "Title lookup unavailable. Try again." }
                                finally { loading = false }
                            }
                        }, enabled = !loading) { Text("${title.name} · ${title.releaseInfo ?: "Date unknown"} · Add") }
                    }
                    if (results.isNotEmpty()) item { TextButton(enabled = !loading, onClick = { search(page + 1) }) { Text("Load more") } }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}
