package com.nuvio.app.features.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.app.features.details.MetaScreenSettingsRepository
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.app.core.format.formatReleaseDateForDisplay
import com.nuvio.app.core.i18n.localizedMediaStatusLabel
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.formatRuntimeForDisplay
import nuvio.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun DetailAdditionalInfoSection(
    meta: MetaDetails,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val settings by MetaScreenSettingsRepository.uiState.collectAsStateWithLifecycle()
    val isSeriesLike = meta.type == "series" || meta.videos.any { it.season != null || it.episode != null }
    val title = if (isSeriesLike) {
        stringResource(Res.string.details_show_details)
    } else {
        stringResource(Res.string.details_movie_details)
    }
    val rows = buildList {
        meta.status?.let {
            add(stringResource(Res.string.details_status) to localizedMediaStatusLabel(it, isSeriesLike))
        }
        meta.releaseInfo?.let {
            add(stringResource(Res.string.details_release_info) to formatReleaseDateForDisplay(it))
        }
        formatRuntimeForDisplay(meta.runtime)?.let {
            add(stringResource(Res.string.details_runtime) to it)
        }
        meta.ageRating?.let { add(stringResource(Res.string.details_certification) to it) }
        meta.country?.let { add(stringResource(Res.string.details_origin_country) to it) }
        meta.language?.let {
            add(stringResource(Res.string.details_original_language) to it.uppercase())
        }
        meta.genres.takeIf { it.isNotEmpty() }?.let { add("Genres" to it.joinToString(", ")) }
        meta.director.takeIf { it.isNotEmpty() }?.let { add("Director" to it.joinToString(", ")) }
        meta.writer.takeIf { it.isNotEmpty() }?.let { add("Writer" to it.joinToString(", ")) }
        meta.imdbRating?.let { add("IMDb rating" to it) }
        meta.productionCompanies.takeIf { it.isNotEmpty() }?.let { add("Production" to it.joinToString(", ") { company -> company.name }) }
        meta.networks.takeIf { it.isNotEmpty() }?.let { add("Network" to it.joinToString(", ") { network -> network.name }) }
    }
    if (rows.isEmpty()) return

    DetailSection(
        title = title,
        modifier = modifier,
        showHeader = showHeader,
    ) {
        if (settings.compactDetails) {
            rows.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { (label, value) ->
                        Column(Modifier.weight(1f).padding(vertical = 5.dp)) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        } else rows.forEachIndexed { index, (label, value) ->
            DetailInfoRow(
                label = label,
                value = value,
                showDivider = index < rows.lastIndex,
            )
        }
        FinancialsSection(meta)
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    showDivider: Boolean,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            )
        }
    }
}
