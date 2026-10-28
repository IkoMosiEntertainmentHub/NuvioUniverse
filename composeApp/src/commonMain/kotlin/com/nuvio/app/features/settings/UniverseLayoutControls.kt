package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.nuvio.app.features.details.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.core.ui.Chip
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import com.nuvio.app.features.details.UniverseSortOrder
import com.nuvio.app.features.details.UniversePosterStyle
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun UniverseLayoutControls(isTablet: Boolean) {
    val settings by MetaScreenSettingsRepository.uiState.collectAsStateWithLifecycle()
    var showDiscovery by remember { mutableStateOf(false) }
    if (showDiscovery) UniverseDiscoveryDialog { showDiscovery = false }
    Column(Modifier.padding(bottom = 12.dp)) {
        TextButton(onClick = { showDiscovery = true }) { Text("Discover and manage universes") }
        Text("Display Universe features", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        UniverseFeature.entries.forEach { feature ->
            SettingsSwitchRow(
                title = feature.label,
                description = if (feature == UniverseFeature.FranchiseCollections) "Generated browsing only; your saved collections are unchanged." else null,
                checked = settings.universeConfiguration.enabled(feature),
                isTablet = isTablet,
                onCheckedChange = { MetaScreenSettingsRepository.setUniverseConfiguration(
                    MetaScreenSettingsRepository.uiState.value.universeConfiguration.withFeature(feature, it)) },
            )
        }
        SettingsSwitchRow(title = "Hide Unreleased Titles", description = "Hide upcoming titles and titles without a confirmed release from generated Universe listings.",
            checked = settings.universeConfiguration.hideUnreleased, isTablet = isTablet,
            onCheckedChange = { MetaScreenSettingsRepository.setUniverseConfiguration(
                MetaScreenSettingsRepository.uiState.value.universeConfiguration.copy(hideUnreleased = it)) })

        SettingsChipRow(
            title = stringResource(Res.string.universe_sort_title),
            description = stringResource(Res.string.universe_sort_description),
            isTablet = isTablet,
        ) {
            UniverseSortOrder.entries.forEach { order ->
                val label = when (order) {
                    UniverseSortOrder.AiredOldest -> Res.string.universe_sort_oldest
                    UniverseSortOrder.AiredNewest -> Res.string.universe_sort_newest
                    UniverseSortOrder.MostPopular -> Res.string.universe_sort_popular
                }
                Chip(label = stringResource(label), selected = settings.universeSortOrder == order,
                    onClick = { MetaScreenSettingsRepository.setUniverseSortOrder(order) })
            }
        }
        SettingsChipRow(
            title = stringResource(Res.string.universe_style_title),
            description = stringResource(Res.string.universe_style_description),
            isTablet = isTablet,
        ) {
            UniversePosterStyle.entries.forEach { style ->
                Chip(label = stringResource(if (style == UniversePosterStyle.Landscape) Res.string.universe_style_landscape else Res.string.universe_style_posters),
                    selected = settings.universePosterStyle == style,
                    onClick = { MetaScreenSettingsRepository.setUniversePosterStyle(style) })
            }
        }
    }
}
