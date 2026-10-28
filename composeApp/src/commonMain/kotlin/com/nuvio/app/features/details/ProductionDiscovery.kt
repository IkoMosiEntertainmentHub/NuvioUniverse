package com.nuvio.app.features.details

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tmdb.buildTmdbUrl
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlinx.serialization.json.*

internal data class DiscoveryPerson(val id: Int, val name: String)
internal data class CreditProduction(val item: MetaPreview, val character: String)

/** Uses Nuvio's configured TMDB key and normal tmdb: title IDs. Never infers franchise links from credits. */
internal object ProductionDiscovery {
    private val requests = Semaphore(4)
    private val verifiedCache = linkedMapOf<String, Pair<Long, UniverseConnection>>()
    private val cacheGate = Mutex()

    private suspend fun fetch(endpoint: String, query: Map<String, String> = emptyMap()): JsonObject = requests.withPermit {
        withTimeout(12_000) {
            val settings = TmdbSettingsRepository.snapshot()
            val params = query + mapOf("language" to settings.language, "include_adult" to "false")
            val url = buildTmdbUrl(endpoint, TmdbSettingsRepository.effectiveApiKey(), params.mapValues { it.value.encodeURLParameter() })
            Json.parseToJsonElement(httpGetText(url)).jsonObject.also {
                if (it["success"]?.jsonPrimitive?.booleanOrNull == false) error("Metadata service unavailable")
            }
        }
    }

    suspend fun titles(query: String, page: Int = 1): List<MetaPreview> =
        fetch("search/multi", mapOf("query" to query.trim(), "page" to page.toString()))["results"]
            ?.jsonArray.orEmpty().mapNotNull { parseDiscoveryTitle(it.jsonObject) }

    suspend fun people(query: String): List<DiscoveryPerson> =
        fetch("search/person", mapOf("query" to query.trim()))["results"]?.jsonArray.orEmpty().mapNotNull {
            val row = it.jsonObject
            val id = row["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val name = row["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            DiscoveryPerson(id, name)
        }

    suspend fun credits(person: Int): List<CreditProduction> =
        fetch("person/$person/combined_credits")["cast"]?.jsonArray.orEmpty().mapNotNull {
            val row = it.jsonObject
            parseDiscoveryTitle(row)?.let { item -> CreditProduction(item, row["character"]?.jsonPrimitive?.contentOrNull.orEmpty()) }
        }.distinctBy { "${it.item.type}:${it.item.id}:${it.character}" }

    suspend fun sharedCredits(first: Int, second: Int): List<MetaPreview> = coroutineScope {
        val left = async { credits(first) }
        val right = async { credits(second) }
        val keys = right.await().map { "${it.item.type}:${it.item.id}" }.toSet()
        left.await().map { it.item }.filter { "${it.type}:${it.id}" in keys }.distinctBy { "${it.type}:${it.id}" }
    }

    suspend fun characters(query: String, cast: List<MetaPerson>): List<MetaPreview> = coroutineScope {
        cast.mapNotNull { it.tmdbId }.distinct().take(20).map { person -> async {
            try { credits(person) } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
        } }.awaitAll().flatten().filter { characterMatches(it.character, query) }
            .map { it.item }.distinctBy { "${it.type}:${it.id}" }
    }

    /** Status is fetched separately; a future/unknown date is never proof of release or an announcement. */
    suspend fun verify(connection: UniverseConnection): UniverseConnection? {
        val key = "${connection.item.type}:${connection.item.id}"
        cacheGate.withLock {
            verifiedCache[key]?.takeIf { Clock.System.now().toEpochMilliseconds() - it.first < 30 * 60_000 }
                ?.let { return connection.copy(item = it.second.item) }
        }
        val id = TmdbService.ensureTmdbId(connection.item.id, connection.item.type) ?: return null
        val type = TmdbService.normalizeMediaType(connection.item.type)
        val data = fetch("$type/$id")
        val name = data.text("title") ?: data.text("name") ?: return null
        if (!establishedTitle(name)) return null
        val status = data.text("status") ?: return null
        // Planned metadata alone is not confirmation of an announced project.
        if (status !in setOf("Released", "Returning Series", "Ended", "Canceled", "In Production", "Post Production", "In Development", "Pilot")) return null
        val date = data.text("release_date") ?: data.text("first_air_date")
        val result = connection.copy(item = connection.item.copy(status = status, rawReleaseDate = date,
            releaseInfo = date?.take(4) ?: connection.item.releaseInfo,
            poster = connection.item.poster ?: data.text("poster_path")?.let { "https://image.tmdb.org/t/p/w500$it" }))
        cacheGate.withLock {
            verifiedCache[key] = Clock.System.now().toEpochMilliseconds() to result
            while (verifiedCache.size > 256) verifiedCache.remove(verifiedCache.keys.first())
        }
        return result
    }
}

internal fun characterMatches(role: String, query: String): Boolean = query.trim().length >= 2 &&
    role.split(" / ", "/", "(").any { it.trim().contains(query.trim(), ignoreCase = true) }

internal fun parseDiscoveryTitle(row: JsonObject): MetaPreview? {
    val media = row.text("media_type") ?: return null
    if (media !in setOf("movie", "tv") || row["adult"]?.jsonPrimitive?.booleanOrNull == true) return null
    val id = row["id"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 } ?: return null
    val name = row.text("title") ?: row.text("name") ?: return null
    if (!establishedTitle(name)) return null
    val date = row.text("release_date") ?: row.text("first_air_date")
    return MetaPreview(id = "tmdb:$id", type = if (media == "tv") "series" else "movie", name = name,
        rawReleaseDate = date, releaseInfo = date?.take(4),
        poster = row.text("poster_path")?.let { "https://image.tmdb.org/t/p/w500$it" })
}

private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
