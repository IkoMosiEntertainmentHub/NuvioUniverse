package com.nuvio.app.features.details

import com.nuvio.app.features.addons.httpGetText
import com.nuvio.app.features.tmdb.TmdbService
import com.nuvio.app.features.tmdb.TmdbSettingsRepository
import com.nuvio.app.features.tmdb.buildTmdbUrl
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import kotlin.time.Clock

internal data class GrossPoint(val date: String, val gross: Long, val cumulative: Long?)
internal data class ProductionCost(val title: String, val level: String, val amount: Double, val currency: String, val source: String?, val date: String?)
internal data class Financials(
    val budget: Long? = null, val domestic: Long? = null, val international: Long? = null,
    val worldwide: Long? = null, val opening: Long? = null,
    val daily: List<GrossPoint> = emptyList(), val weekly: List<GrossPoint> = emptyList(), val weekends: List<GrossPoint> = emptyList(),
    val source: String, val sourceUrl: String, val captured: String? = null,
    val productionCosts: List<ProductionCost> = emptyList(),
) {
    val grossMinusBudget: Long? get() = if (worldwide != null && budget != null) worldwide - budget else null
    val budgetMultiple: Double? get() = if (worldwide != null && budget != null && budget > 0) worldwide.toDouble() / budget else null
}

internal object FinancialsRepository {
    private val cache = linkedMapOf<String, Pair<Long, Financials>>()
    private val gate = Mutex()
    suspend fun load(meta: MetaDetails): Financials {
        val key = "${meta.type}:${meta.id}"
        gate.withLock { cache[key]?.takeIf { Clock.System.now().toEpochMilliseconds() - it.first < 24 * 60 * 60_000 }?.let { return it.second } }
        val movie = meta.type in setOf("movie", "film")
        val imdb = universeImdbId(meta.imdbId) ?: universeImdbId(meta.id)
        val tmdb = universeTmdbId(meta.id) ?: TmdbService.ensureTmdbId(meta.id, meta.type, meta.imdbId)?.toIntOrNull()
        val archive = if (movie) safely {
            val lookup = if (imdb != null) "imdb/$imdb" else tmdb?.let { "tmdb/$it" } ?: return@safely null
            val found = json("https://boxofficehistory.com/api/v1/lookup/$lookup.json")
            val id = found.text("id")?.takeIf { Regex("[a-zA-Z0-9_-]+").matches(it) } ?: return@safely null
            parseArchiveFinancials(json("https://boxofficehistory.com/api/v1/movies/$id.json"))
        } else null
        val tmdbData = if (movie && (archive?.budget == null || archive.worldwide == null)) safely {
            tmdb?.let { json(buildTmdbUrl("movie/$it", TmdbSettingsRepository.effectiveApiKey())) }
        } else null
        val costs = if (imdb != null && (!movie || archive?.budget == null && tmdbData?.money("budget") == null))
            safely { wikidataCosts(imdb, movie) }.orEmpty() else emptyList()
        // Do not silently combine totals from different reporting snapshots.
        val result = archive?.copy(productionCosts = costs) ?: Financials(
            budget = tmdbData?.money("budget"), worldwide = tmdbData?.money("revenue"),
            source = if (tmdbData != null) "TMDB reported movie figures" else "No comparable financial figures available",
            sourceUrl = tmdb?.let { "https://www.themoviedb.org/${if (movie) "movie" else "tv"}/$it" } ?: "https://boxofficehistory.com/api",
            productionCosts = costs,
        )
        gate.withLock {
            cache[key] = Clock.System.now().toEpochMilliseconds() to result
            while (cache.size > 64) cache.remove(cache.keys.first())
        }
        return result
    }

    private suspend fun json(url: String): JsonObject = withTimeout(12_000) { Json.parseToJsonElement(httpGetText(url)).jsonObject }
    private suspend fun <T> safely(block: suspend () -> T): T? = try { block() } catch (e: CancellationException) { throw e } catch (_: Exception) { null }

    private suspend fun wikidataCosts(imdb: String, movie: Boolean): List<ProductionCost> {
        require(Regex("tt\\d+").matches(imdb))
        val query = """
            SELECT ?root ?production ?productionLabel ?kind ?amount ?currencyCode ?source ?date WHERE {
              ?root wdt:P345 "$imdb" .
              { BIND(?root AS ?production) } UNION { ?production (wdt:P179|wdt:P4908)/(wdt:P179|wdt:P4908)? ?root . }
              OPTIONAL { ?production wdt:P31 ?kind }
              ?production p:P2130 ?statement . ?statement psv:P2130 ?value .
              ?value wikibase:quantityAmount ?amount ; wikibase:quantityUnit ?currency .
              OPTIONAL { ?currency wdt:P498 ?currencyCode }
              OPTIONAL { ?statement prov:wasDerivedFrom/pr:P854 ?source }
              OPTIONAL { ?statement pq:P585 ?date }
              SERVICE wikibase:label { bd:serviceParam wikibase:language "en". }
            } LIMIT 100
        """.trimIndent()
        val payload = json("https://query.wikidata.org/sparql?format=json&query=${query.encodeURLParameter()}")
        return payload["results"]?.jsonObject?.get("bindings")?.jsonArray.orEmpty().mapNotNull { row ->
            val data = row.jsonObject
            fun value(name: String) = data[name]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
            val amount = value("amount")?.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
            val level = when {
                value("production") == value("root") -> if (movie) "Movie" else "Series"
                value("kind")?.endsWith("/Q21191270") == true -> "Episode"
                value("kind")?.endsWith("/Q3464665") == true -> "Season"
                else -> "Production"
            }
            ProductionCost(value("productionLabel") ?: return@mapNotNull null, level, amount,
                value("currencyCode") ?: "Currency unknown", value("source"), value("date")?.take(10))
        }.distinct()
    }
}

internal fun parseArchiveFinancials(data: JsonObject): Financials {
    fun points(key: String) = (data[key] as? JsonArray).orEmpty().mapNotNull { row ->
        val obj = row as? JsonObject ?: return@mapNotNull null
        val date = obj.text("date") ?: obj.text("start") ?: return@mapNotNull null
        val gross = (obj["gross"] as? JsonPrimitive)?.longOrNull?.takeIf { it >= 0 } ?: return@mapNotNull null
        GrossPoint(date, gross, (obj["toDate"] as? JsonPrimitive)?.longOrNull?.takeIf { it >= 0 })
    }.sortedBy { it.date }.distinctBy { it.date to it.gross }
    val id = data.text("id").orEmpty()
    val captured = (data["sources"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.text("captured") }.maxOrNull()
    return Financials(data.money("budget"), data.money("domestic"), data.money("foreign"), data.money("worldwide"),
        (data["opening"] as? JsonObject)?.money("gross"), points("daily"), points("weeks"), points("weekends"),
        "Box Office History, from the Internet Archive's copies of Box Office Mojo", "https://boxofficehistory.com/api/v1/movies/$id.json", captured)
}

private fun JsonObject.text(key: String): String? = (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
private fun JsonObject.money(key: String): Long? = (get(key) as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }

internal fun knownProductionCostTotal(costs: List<ProductionCost>): Pair<Double, String>? {
    // Conflicting statements about one production, mixed currencies, and aggregate + episode double-counting are not summed.
    if (costs.isEmpty() || costs.any { it.level != "Episode" } || costs.map { it.currency }.distinct().size != 1 || costs.any { it.currency == "Currency unknown" } ||
        costs.map { it.title }.distinct().size != costs.size) return null
    return costs.sumOf { it.amount } to costs.first().currency
}
