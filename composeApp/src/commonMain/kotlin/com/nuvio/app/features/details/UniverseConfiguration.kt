package com.nuvio.app.features.details

import kotlinx.serialization.Serializable

enum class UniverseFeature(val label: String) {
    Continuations("Follow / Followed By"),
    SpinOffs("Spin-Offs / Spin-Offs From"),
    FranchiseCollections("Franchise Collections"),
    Prequels("Prequels"),
    Crossovers("Crossovers"),
    Related("Related Titles"),
}

@Serializable
data class UniverseSelection(val id: String, val type: String, val name: String, val enabled: Boolean = true)

@Serializable
data class UniverseConfiguration(
    val continuations: Boolean = true,
    val spinOffs: Boolean = true,
    val franchiseCollections: Boolean = false,
    val prequels: Boolean = true,
    val crossovers: Boolean = true,
    val related: Boolean = true,
    val hideUnreleased: Boolean = false,
    val selections: List<UniverseSelection> = emptyList(),
) {
    fun enabled(feature: UniverseFeature): Boolean = when (feature) {
        UniverseFeature.Continuations -> continuations
        UniverseFeature.SpinOffs -> spinOffs
        UniverseFeature.FranchiseCollections -> franchiseCollections
        UniverseFeature.Prequels -> prequels
        UniverseFeature.Crossovers -> crossovers
        UniverseFeature.Related -> related
    }

    fun withFeature(feature: UniverseFeature, enabled: Boolean): UniverseConfiguration = when (feature) {
        UniverseFeature.Continuations -> copy(continuations = enabled)
        UniverseFeature.SpinOffs -> copy(spinOffs = enabled)
        UniverseFeature.FranchiseCollections -> copy(franchiseCollections = enabled)
        UniverseFeature.Prequels -> copy(prequels = enabled)
        UniverseFeature.Crossovers -> copy(crossovers = enabled)
        UniverseFeature.Related -> copy(related = enabled)
    }
}

internal fun universeFeature(relation: String): UniverseFeature? = when (relation.lowercase()) {
    "follows", "followed by" -> UniverseFeature.Continuations
    "spin-off", "spin-off from" -> UniverseFeature.SpinOffs
    "prequel", "prequel to" -> UniverseFeature.Prequels
    "crossover", "crossover with" -> UniverseFeature.Crossovers
    "remake of", "remade as", "franchise connection" -> UniverseFeature.Related
    else -> null
}

/** Unknown dates/statuses never prove release. No library or history mutation occurs here. */
internal fun UniverseConnection.isReleased(today: String): Boolean {
    val status = item.status?.lowercase()
    if (status in setOf("planned", "in production", "post production", "announced", "filming", "in development")) return false
    val date = item.rawReleaseDate
    if (date != null && Regex("\\d{4}-\\d{2}-\\d{2}").matches(date)) return date <= today
    return status in setOf("released", "returning series", "ended")
}

internal fun visibleUniverseConnections(
    items: List<UniverseConnection>, configuration: UniverseConfiguration, today: String,
): List<UniverseConnection> = items.mapNotNull { connection ->
    if (configuration.hideUnreleased && !connection.isReleased(today)) return@mapNotNull null
    val relations = connection.relations.filter { relation ->
        universeFeature(relation)?.let(configuration::enabled) == true
    }
    connection.copy(relations = relations).takeIf { relations.isNotEmpty() }
}

internal fun establishedTitle(name: String): Boolean = name.isNotBlank() &&
    !Regex("(?i)\\b(rumou?red|untitled|tba|tbd)\\b").containsMatchIn(name)
