package com.nuvio.app.features.details

import com.nuvio.app.features.home.MetaPreview

/** Small reviewed supplement: release-order IMDb edges do NOT establish story chronology.
 * Each pair has an explicit primary-source reference. No actor/studio-based inference.
 * This is deliberately not a claim of complete worldwide prequel/crossover coverage.
 */
private data class EvidenceTitle(val id: String, val name: String)
private data class EvidenceEdge(val from: EvidenceTitle, val to: EvidenceTitle, val relation: String, val reverse: String, val source: String)
private val evidenceEdges = listOf(
    EvidenceEdge(EvidenceTitle("tt0120915", "Star Wars: Episode I - The Phantom Menace"),
        EvidenceTitle("tt0076759", "Star Wars: Episode IV - A New Hope"), "Prequel to", "Prequel",
        "https://www.starwars.com/films/star-wars-episode-i-the-phantom-menace"),
    EvidenceEdge(EvidenceTitle("tt0329101", "Freddy vs. Jason"),
        EvidenceTitle("tt0087800", "A Nightmare on Elm Street"), "Crossover with", "Crossover",
        "https://www.warnerbros.com/movies/freddy-vs-jason"),
    EvidenceEdge(EvidenceTitle("tt0329101", "Freddy vs. Jason"),
        EvidenceTitle("tt0080761", "Friday the 13th"), "Crossover with", "Crossover",
        "https://www.warnerbros.com/movies/freddy-vs-jason"),
)

/** An announcement can establish a planned record, but never invent a missing date or status.
 * Stable IMDb identity and matching title are required; a name match alone is insufficient.
 */
internal fun officialAnnouncementSource(id: String, title: String): String? = when {
    id == "tt21357150" && title.equals("Avengers: Doomsday", ignoreCase = true) ->
        "https://movies.disney.com/avengers-doomsday"
    else -> null
}

internal fun explicitUniverseConnections(id: String): List<UniverseConnection> = evidenceEdges.mapNotNull { edge ->
    val (title, relation) = when (id) {
        edge.from.id -> edge.to to edge.relation
        edge.to.id -> edge.from to edge.reverse
        else -> return@mapNotNull null
    }
    UniverseConnection(MetaPreview(title.id, "movie", title.name), listOf(relation), edge.source)
}

internal fun mergeUniverseEvidence(provider: List<UniverseConnection>, explicit: List<UniverseConnection>): List<UniverseConnection> {
    val merged = provider.associateByTo(linkedMapOf()) { it.item.id }
    explicit.forEach { entry ->
        val old = merged[entry.item.id]
        merged[entry.item.id] = if (old == null) entry else old.copy(
            relations = (old.relations + entry.relations).distinct(), evidence = old.evidence + "; " + entry.evidence)
    }
    return merged.values.toList()
}
