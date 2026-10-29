package com.nuvio.app.features.player.skip

import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

internal data class SkipProviderResult(val intervals: List<SkipInterval> = emptyList(), val confirmedAbsent: Set<String> = emptySet())

internal fun skipCategory(type: String): String? = when (type.lowercase()) {
    "intro", "op", "mixed-op" -> "intro"
    "recap" -> "recap"
    "outro", "ed", "mixed-ed", "credits", "ending", "movie-credits" -> "outro"
    "post-credits" -> "post-credits"
    else -> null
}

internal fun mergeSkipProviders(vararg results: List<SkipInterval>): List<SkipInterval> {
    val chosen = linkedMapOf<String, List<SkipInterval>>()
    results.forEach { provider ->
        provider.filter { it.startTime.isFinite() && it.endTime.isFinite() && it.startTime >= 0 && it.endTime > it.startTime }
            .groupBy { skipCategory(it.type) }.forEach { (category, intervals) ->
                if (category != null && category !in chosen) chosen[category] = intervals.distinctBy { it.startTime to it.endTime }
            }
    }
    return chosen.values.flatten().sortedBy { it.startTime }
}

/** Only missing categories are requested. Never replace a valid primary marker with a fallback. */
internal suspend fun completeSkipSegments(
    primary: List<SkipInterval>,
    skipDb: suspend (String) -> SkipProviderResult,
    theIntroDb: suspend () -> List<SkipInterval>,
): List<SkipInterval> {
    val required = setOf("intro", "recap", "outro")
    val first = mergeSkipProviders(primary)
    val missing = required - first.mapNotNull { skipCategory(it.type) }.toSet()
    if (missing.isEmpty()) return first
    val secondary = coroutineScope { missing.map { kind -> async { skipDb(kind) } }.awaitAll() }
    val merged = mergeSkipProviders(first, secondary.flatMap { it.intervals })
    val absent = secondary.flatMap { it.confirmedAbsent }.toSet()
    val remaining = required - merged.mapNotNull { skipCategory(it.type) }.toSet() - absent
    if (remaining.isEmpty()) return merged
    val third = theIntroDb().filter { skipCategory(it.type) in remaining }
    return mergeSkipProviders(merged, third)
}

internal object SkipFallbackApi {
    suspend fun complete(primary: List<SkipInterval>, imdb: String, season: Int? = null, episode: Int? = null, durationMs: Long? = null): List<SkipInterval> {
        if (!Regex("tt\\d+").matches(imdb) || (season == null) != (episode == null)) return primary
        val coordinates = if (season != null) "&season=$season&episode=$episode" else ""
        val seconds = durationMs?.takeIf { it > 0 }?.let { "&duration=${it / 1000.0}" }.orEmpty()
        return completeSkipSegments(primary,
            skipDb = { type ->
                val body = get("https://api.skipdb.tv/api/segments?imdb_id=$imdb$coordinates&type=$type$seconds&adjust=conservative")
                body?.let { parseSkipDb(it, type, season == null, durationMs) } ?: SkipProviderResult()
            },
            theIntroDb = {
                val duration = durationMs?.takeIf { it > 0 }?.let { "&duration_ms=$it" }.orEmpty()
                get("https://api.theintrodb.org/v4/media?imdb_id=$imdb$coordinates$duration")
                    ?.let { parseTheIntroDb(it, season == null, durationMs) }.orEmpty()
            })
    }
    private suspend fun get(url: String): String? = try {
        withTimeoutOrNull(8_000) { httpGetText(url) }
    } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
}

internal fun parseSkipDb(body: String, kind: String, movie: Boolean, durationMs: Long?): SkipProviderResult = try {
    val root = Json.parseToJsonElement(body).jsonObject
    val segment = (root["segments"] as? JsonObject)?.get(kind) as? JsonObject
    if (segment == null || segment["match"]?.jsonPrimitive?.contentOrNull == "out-of-range") SkipProviderResult()
    else {
        val start = (segment["start_ms"] as? JsonPrimitive)?.doubleOrNull
        val end = (segment["end_ms"] as? JsonPrimitive)?.doubleOrNull
        if (start == 0.0 && end == 0.0) SkipProviderResult(confirmedAbsent = setOf(kind))
        else SkipProviderResult(listOfNotNull(segmentInterval(start, end, kind, movie, "skipdb", durationMs)))
    }
} catch (_: Exception) { SkipProviderResult() }

internal fun parseTheIntroDb(body: String, movie: Boolean, durationMs: Long?): List<SkipInterval> = try {
    val root = Json.parseToJsonElement(body).jsonObject
    listOf("intro", "recap", "credits").flatMap { key ->
        (root[key] as? JsonArray).orEmpty().mapNotNull { row ->
            val segment = row as? JsonObject ?: return@mapNotNull null
            val start = (segment["start_ms"] as? JsonPrimitive)?.doubleOrNull ?: if (key == "intro") 0.0 else return@mapNotNull null
            val end = (segment["end_ms"] as? JsonPrimitive)?.doubleOrNull ?: if (key == "credits") durationMs?.toDouble() else null
            segmentInterval(start, end, if (key == "credits") "outro" else key, movie, "theintrodb", durationMs)
        }
    }
} catch (_: Exception) { emptyList() }

private fun segmentInterval(start: Double?, end: Double?, kind: String, movie: Boolean, provider: String, durationMs: Long?): SkipInterval? {
    if (start == null || end == null || !start.isFinite() || !end.isFinite() || start < 0 || end <= start ||
        durationMs != null && durationMs > 0 && end > durationMs + 1000) return null
    return SkipInterval(start / 1000, end / 1000, if (movie && kind == "outro") "movie-credits" else kind, provider)
}
