package com.nuvio.app.features.details

import com.nuvio.app.features.addons.RawHttpResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class UniverseRepositoryTest {
    private val meta = MetaDetails(id = "tt0903747", type = "series", name = "A show")

    @Test fun `connections retain multiple relations and skip self and unsupported types`() {
        val result = parseUniverse(payload(
            edge("tt0903747", "Self", "Follows", "tvSeries"),
            edge("tt3032476", "Spin-off", "Followed by", "tvSeries"),
            edge("tt3032476", "Spin-off", "Spin-off", "tvSeries"),
            edge("tt9243946", "TV movie", "Followed by", "tvMovie"),
            edge("tt123", "Episode", "Follows", "tvEpisode"),
            edge("bad", "Invalid", "Follows", "movie"),
        ), meta.id)!!
        assertEquals(listOf("tt3032476", "tt9243946"), result.map { it.item.id })
        assertEquals(listOf("Followed by", "Spin-off"), result.first().relations)
        assertEquals("series", result.first().item.type)
        assertEquals("movie", result.last().item.type)
        assertEquals("2015", result.first().item.releaseInfo)
        assertEquals("2015-02-08", result.first().item.rawReleaseDate)
        assertEquals("https://example.org/poster.jpg", result.first().item.poster)
    }

    @Test fun `nulls and malformed edge records do not crash parsing`() {
        val root = """{"data":{"title":{"connections":{"edges":[null,{}, {"node":null}, {"node":{"associatedTitle":{"id":"tt123"}}}]}}}}"""
        assertEquals(emptyList(), parseUniverse(root, meta.id))
        assertNull(parseUniverse("""{"data":{"title":null}}""", meta.id))
        assertNull(parseUniverse("""{"errors":[{"message":"Blocked"}],"data":null}""", meta.id))
    }

    @Test fun `valid results are cached and expire`() = runTest {
        var now = 0L
        var requests = 0
        val client = UniverseClient(
            request = { body ->
                requests++
                val json = Json.parseToJsonElement(body).jsonObject
                assertEquals(meta.id, json["variables"]!!.jsonObject["id"]!!.jsonPrimitive.content)
                assertTrue(json["query"]!!.jsonPrimitive.content.contains("connections(first: 50"))
                response(payload(edge("tt3032476", "Spin-off", "Spin-off", "tvSeries")))
            },
            resolveImdb = { _, _ -> error("Should use existing IMDb ID") },
            nowMs = { now },
        )
        assertEquals(1, client.load(meta)!!.size)
        client.load(meta)
        assertEquals(1, requests)
        now = 31 * 60_000L
        client.load(meta)
        assertEquals(2, requests)
    }

    @Test fun `TMDB IDs resolve through existing metadata service hook`() = runTest {
        var resolvedType = ""
        val client = UniverseClient(
            request = { response(payload()) },
            resolveImdb = { id, type -> assertEquals(1396, id); resolvedType = type; meta.id },
            nowMs = { 0L },
        )
        assertEquals(emptyList(), client.load(meta.copy(id = "tmdb:1396")))
        assertEquals("series", resolvedType)
    }

    @Test fun `transient failure is not cached and can retry`() = runTest {
        var requests = 0
        val client = UniverseClient(
            request = { requests++; if (requests == 1) response("forbidden", 403) else response(payload()) },
            resolveImdb = { _, _ -> null }, nowMs = { 0L },
        )
        assertNull(client.load(meta))
        assertEquals(emptyList(), client.load(meta))
        assertEquals(2, requests)
    }

    @Test fun `GraphQL failures and malformed JSON remain unavailable`() = runTest {
        for (body in listOf("<html>blocked</html>", """{"errors":[{"message":"No"}]}""")) {
            val client = UniverseClient(request = { response(body) }, resolveImdb = { _, _ -> null }, nowMs = { 0L })
            assertNull(client.load(meta))
        }
    }

    @Test fun `timeout returns unavailable but external cancellation propagates`() = runTest {
        val timeout = UniverseClient(request = { delay(200); response(payload()) }, resolveImdb = { _, _ -> null }, nowMs = { 0L }, timeoutMs = 100)
        assertNull(timeout.load(meta))
        val cancelled = UniverseClient(request = { throw CancellationException("Leaving screen") }, resolveImdb = { _, _ -> null }, nowMs = { 0L })
        assertFailsWith<CancellationException> { cancelled.load(meta) }
    }

    @Test fun `cache stays bounded`() = runTest {
        var requests = 0
        val client = UniverseClient(request = { requests++; response(payload()) }, resolveImdb = { _, _ -> null }, nowMs = { 0L })
        repeat(65) { client.load(meta.copy(id = "tt${1000 + it}")) }
        client.load(meta.copy(id = "tt1000"))
        assertEquals(66, requests)
    }

    @Test fun `unsupported content never makes a network request`() = runTest {
        val client = UniverseClient(request = { error("Unexpected request") }, resolveImdb = { _, _ -> error("Unexpected lookup") }, nowMs = { 0L })
        assertEquals(emptyList(), client.load(meta.copy(type = "channel")))
        assertEquals(emptyList(), client.load(meta.copy(id = "mal:123")))
        assertNull(universeImdbId("ttbad"))
        assertEquals("tt123", universeImdbId("imdb:tt123:1:2"))
    }

    private fun response(body: String, status: Int = 200) = RawHttpResponse(status, "", "https://graphql.imdb.com/", body, emptyMap())
    private fun payload(vararg edges: JsonObject): String = buildJsonObject {
        putJsonObject("data") { putJsonObject("title") { putJsonObject("connections") {
            put("edges", JsonArray(edges.toList()))
        } } }
    }.toString()

    private fun edge(id: String, name: String, relation: String, type: String) = buildJsonObject {
        putJsonObject("node") {
            putJsonObject("category") { put("text", relation) }
            putJsonObject("associatedTitle") {
                put("id", id)
                putJsonObject("titleText") { put("text", name) }
                putJsonObject("titleType") { put("id", type) }
                putJsonObject("primaryImage") { put("url", "https://example.org/poster.jpg") }
                putJsonObject("releaseYear") { put("year", 2015) }
                putJsonObject("releaseDate") { put("year", 2015); put("month", 2); put("day", 8) }
            }
        }
    }
}
