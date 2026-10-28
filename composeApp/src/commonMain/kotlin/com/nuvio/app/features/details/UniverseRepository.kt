package com.nuvio.app.features.details

import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.addons.httpRequestRaw
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.tmdb.TmdbService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import kotlin.time.Clock

/** A title can have several relationships to the current movie or show. */
internal data class UniverseConnection(val item: MetaPreview, val relations: List<String>)

internal object UniverseRepository {
    private val client = UniverseClient(
        request = { body ->
            httpRequestRaw(
                method = "POST",
                url = "https://graphql.imdb.com/",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Accept" to "application/json",
                    "User-Agent" to "Mozilla/5.0",
                    "Referer" to "https://www.imdb.com/",
                ),
                body = body,
            )
        },
        resolveImdb = { id, type -> TmdbService.tmdbToImdb(id, type) },
        nowMs = { Clock.System.now().toEpochMilliseconds() },
    )

    suspend fun load(meta: MetaDetails): List<UniverseConnection>? = client.load(meta)
}

/** Null means unavailable; an empty list means the provider returned no connections. */
internal class UniverseClient(
    private val request: suspend (String) -> RawHttpResponse,
    private val resolveImdb: suspend (Int, String) -> String?,
    private val nowMs: () -> Long,
    private val timeoutMs: Long = 6_000,
) {
    private data class Cached(val at: Long, val items: List<UniverseConnection>)
    private val mutex = Mutex()
    private val cache = linkedMapOf<String, Cached>()

    suspend fun load(meta: MetaDetails): List<UniverseConnection>? {
        if (!supportsUniverse(meta.type)) return emptyList()
        return try {
            withTimeoutOrNull(timeoutMs) {
                val imdbId = universeImdbId(meta.imdbId) ?: universeImdbId(meta.id)
                    ?: universeTmdbId(meta.id)?.let { resolveImdb(it, meta.type) }?.let(::universeImdbId)
                    ?: return@withTimeoutOrNull emptyList()
                mutex.withLock {
                    cache[imdbId]?.takeIf { nowMs() - it.at in 0..CACHE_TTL_MS }
                        ?.let { return@withTimeoutOrNull it.items }
                }
                val body = buildJsonObject {
                    put("query", UNIVERSE_QUERY)
                    putJsonObject("variables") { put("id", imdbId) }
                }.toString()
                val response = request(body)
                if (response.status != 200) return@withTimeoutOrNull null
                val items = parseUniverse(response.body, imdbId) ?: return@withTimeoutOrNull null
                mutex.withLock {
                    cache.remove(imdbId)
                    cache[imdbId] = Cached(nowMs(), items)
                    while (cache.size > MAX_CACHE_TITLES) cache.remove(cache.keys.first())
                }
                items
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val CACHE_TTL_MS = 30 * 60_000L
        const val MAX_CACHE_TITLES = 64
    }
}

internal fun supportsUniverse(type: String): Boolean =
    type.lowercase() in setOf("movie", "film", "series", "show", "tv", "tvshow")

internal fun universeImdbId(value: String?): String? = value?.trim()
    ?.split(':', '/', '?', '&')?.firstOrNull { Regex("tt[0-9]+").matches(it) }

internal fun universeTmdbId(value: String): Int? = value.trim().takeIf { it.startsWith("tmdb:") }
    ?.substringAfter(':')?.substringBefore(':')?.toIntOrNull()?.takeIf { it > 0 }

internal fun parseUniverse(payload: String, sourceImdbId: String): List<UniverseConnection>? {
    val root = Json.parseToJsonElement(payload) as? JsonObject ?: return null
    if ((root["errors"] as? JsonArray)?.isNotEmpty() == true) return null
    val title = (root["data"] as? JsonObject)?.get("title") as? JsonObject ?: return null
    val edges = (title["connections"] as? JsonObject)?.get("edges") as? JsonArray ?: return null
    val titles = linkedMapOf<String, UniverseConnection>()
    for (edge in edges) {
        val node = (edge as? JsonObject)?.get("node") as? JsonObject ?: continue
        val associated = node["associatedTitle"] as? JsonObject ?: continue
        val id = universeImdbId(associated.text("id")) ?: continue
        if (id == sourceImdbId) continue
        val name = (associated["titleText"] as? JsonObject)?.text("text") ?: continue
        val relation = (node["category"] as? JsonObject)?.text("text") ?: continue
        val old = titles[id]
        if (old != null) {
            titles[id] = old.copy(relations = (old.relations + relation).distinct())
            continue
        }
        val type = (associated["titleType"] as? JsonObject)?.text("id")
        val series = type in setOf("tvSeries", "tvMiniSeries", "tvShortSeries")
        // Episodes and games cannot be opened as independent movie/series details.
        if (type != null && type !in setOf("movie", "tvMovie", "short", "tvShort", "video", "tvSpecial") && !series) continue
        val year = (associated["releaseYear"] as? JsonObject)?.text("year")
        titles[id] = UniverseConnection(
            item = MetaPreview(
                id = id,
                type = if (series) "series" else "movie",
                name = name,
                poster = (associated["primaryImage"] as? JsonObject)?.text("url"),
                releaseInfo = year,
                rawReleaseDate = (associated["releaseDate"] as? JsonObject)?.let { date ->
                    val y = date.text("year")?.toIntOrNull()
                    val m = date.text("month")?.toIntOrNull()
                    val d = date.text("day")?.toIntOrNull()
                    if (y != null && m != null && d != null) "$y-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}" else null
                },
            ),
            relations = listOf(relation),
        )
    }
    return titles.values.toList()
}

private fun JsonObject.text(key: String): String? =
    (get(key) as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

// Independent Kotlin implementation; no Debrify application code is transplanted.
internal val UNIVERSE_QUERY = """
    query NuvioUniverse(${ '$' }id: ID!) {
      title(id: ${ '$' }id) {
        connections(first: 50, filter: { categories: [
          "followed_by", "follows", "spin_off", "spin_off_from", "remake_of", "remade_as"
        ] }) {
          edges { node {
            category { text }
            associatedTitle {
              id
              titleText { text }
              titleType { id }
              primaryImage { url }
              releaseYear { year }
              releaseDate { year month day }
            }
          } }
        }
      }
    }
""".trimIndent()
