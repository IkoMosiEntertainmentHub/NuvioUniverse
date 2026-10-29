package com.nuvio.app.features.details

import com.nuvio.app.features.home.MetaPreview
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class UniverseConfigurationTest {
    @Test fun `one universe rail combines relationship types without duplicate productions`() {
        val sequel = title("tt2", listOf("Followed by"), "2020-01-01")
        val spinOff = title("tt3", listOf("Spin-off"), "2021-01-01")
        val future = title("tt4", listOf("Prequel"), "2028-01-01")
        val items = listOf(sequel, spinOff, sequel.copy(relations = listOf("Crossover")), future)
        val config = UniverseConfiguration(franchiseCollections = true, hideUnreleased = true)
        val rail = universeRailConnections(items, config, "2026-10-29")
        assertEquals(listOf("tt2", "tt3"), rail.map { it.item.id })
        assertTrue(rail.first().relations.containsAll(listOf("Followed by", "Crossover")))
        val disabled = universeRailConnections(items, config.copy(continuations = false), "2026-10-29")
        assertFalse(disabled.flatMap { it.relations }.contains("Followed by"))
        assertTrue(disabled.any { "Spin-off" in it.relations })
    }

    @Test fun `empty or fully filtered universes do not create an outer padded section`() {
        val meta = MetaDetails("tt1", "movie", "Title")
        assertFalse(hasVisibleUniverse(meta, emptyList(), UniverseConfiguration(), "2026-10-29"))
        val future = UniverseConnection(MetaPreview("tt2", "movie", "Future", rawReleaseDate = "2028-01-01"), listOf("Spin-off"))
        assertFalse(hasVisibleUniverse(meta, listOf(future), UniverseConfiguration(hideUnreleased = true), "2026-10-29"))
        assertTrue(hasVisibleUniverse(meta, listOf(future), UniverseConfiguration(), "2026-10-29"))
        assertFalse(supportedClearLogo("https://example.org/logo.jpg"))
        assertTrue(supportedClearLogo("https://image.tmdb.org/t/p/w500/clear.png"))
    }
    private fun title(id: String, relations: List<String>, date: String? = null, status: String? = null) =
        UniverseConnection(MetaPreview(id, "movie", id, rawReleaseDate = date, status = status), relations)

    @Test fun `every feature toggle is independent and generated collections start off`() {
        val default = UniverseConfiguration()
        assertFalse(default.franchiseCollections)
        for (feature in UniverseFeature.entries) {
            val changed = default.withFeature(feature, !default.enabled(feature))
            for (other in UniverseFeature.entries.filter { it != feature }) assertEquals(default.enabled(other), changed.enabled(other))
            assertEquals(!default.enabled(feature), changed.enabled(feature))
        }
    }

    @Test fun `each disabled relationship is removed but other labels survive`() {
        val all = title("tt1", listOf("Follows", "Spin-off", "Prequel", "Crossover", "Remake of"), "2020-01-01")
        for (feature in UniverseFeature.entries.filter { it != UniverseFeature.FranchiseCollections }) {
            val result = visibleUniverseConnections(listOf(all), UniverseConfiguration().withFeature(feature, false), "2026-10-29").single()
            assertFalse(result.relations.any { universeFeature(it) == feature })
            assertTrue(result.relations.isNotEmpty())
        }
        assertTrue(visibleUniverseConnections(listOf(all), UniverseConfiguration(false, false, false, false, false, false), "2026-10-29").isEmpty())
        assertEquals(5, all.relations.size) // Input metadata is never deleted.
    }

    @Test fun `unreleased filter applies regardless of relationship and unknown date is not released`() {
        for (relation in listOf("Followed by", "Spin-off", "Prequel", "Crossover", "Remade as", "Franchise connection")) {
            val items = listOf(title("released", listOf(relation), "2020-01-01"),
                title("future", listOf(relation), "2028-01-01"), title("unknown", listOf(relation)),
                title("development", listOf(relation), "2020-01-01", "In Production"))
            assertEquals(4, visibleUniverseConnections(items, UniverseConfiguration(), "2026-10-29").size)
            assertEquals(listOf("released"), visibleUniverseConnections(items, UniverseConfiguration(hideUnreleased = true), "2026-10-29").map { it.item.id })
        }
    }

    @Test fun `rumors and placeholder identities are excluded`() {
        listOf("Untitled Marvel project", "TBA", "Rumored film", "TBD movie").forEach { assertFalse(establishedTitle(it)) }
        assertTrue(establishedTitle("Avengers: Doomsday"))
    }

    @Test fun `individual universe switches match both normal details identities without deleting titles`() {
        val selection = UniverseSelection("tt1", "movie", "A title", false, "tmdb:12")
        val config = UniverseConfiguration(selections = listOf(selection))
        assertFalse(universeEnabled(MetaDetails("tmdb:12", "movie", "A title"), config))
        assertFalse(universeEnabled(MetaDetails("tt1", "movie", "A title"), config))
        assertTrue(universeEnabled(MetaDetails("tt2", "movie", "Other"), config))
        assertFalse(config.franchiseCollections)
    }

    @Test fun `co-star intersection uses production identity and not coincidental titles`() {
        val film = MetaPreview("tmdb:1", "movie", "Same name")
        val show = MetaPreview("tmdb:1", "series", "Same name")
        assertEquals(listOf(film), sharedCastCredits(listOf(CreditProduction(film, "A"), CreditProduction(show, "B")), listOf(CreditProduction(film, "C"))))
        assertTrue(sharedCastCredits(listOf(CreditProduction(show, "A")), listOf(CreditProduction(film, "B"))).isEmpty())
    }

    @Test fun `announcements require a confirmed stable identity and never manufacture a date`() {
        assertNotNull(officialAnnouncementSource("tt21357150", "Avengers: Doomsday"))
        assertNull(officialAnnouncementSource("tt1", "Avengers: Doomsday"))
        assertNull(officialAnnouncementSource("tt21357150", "Untitled Avengers"))
        assertFalse(title("tt21357150", listOf("Followed by"), status = "Announced").isReleased("2026-10-29"))
    }

    @Test fun `prequels and crossovers use explicit sourced evidence rather than release order`() {
        assertNull(universeFeature("References"))
        assertEquals(UniverseFeature.Continuations, universeFeature("Follows"))
        val prequel = explicitUniverseConnections("tt0076759").single()
        assertEquals("Prequel", prequel.relations.single())
        assertTrue(prequel.evidence.startsWith("https://www.starwars.com/"))
        assertEquals(2, explicitUniverseConnections("tt0329101").size)
        assertTrue(explicitUniverseConnections("tt999999999").isEmpty())
    }

    @Test fun `character search checks credit roles and title parser excludes people and placeholders`() {
        assertTrue(characterMatches("Tony Stark / Iron Man", "iron man"))
        assertFalse(characterMatches("Bruce Banner", "Iron Man"))
        assertFalse(characterMatches("Tony Stark", ""))
        assertNull(parseDiscoveryTitle(Json.parseToJsonElement("""{"id":1,"media_type":"person","name":"Actor"}""").jsonObject))
        assertNull(parseDiscoveryTitle(Json.parseToJsonElement("""{"id":1,"media_type":"movie","title":"Untitled project"}""").jsonObject))
        val row = parseDiscoveryTitle(Json.parseToJsonElement("""{"id":1,"media_type":"tv","name":"Show","first_air_date":""}""").jsonObject)!!
        assertEquals("series", row.type)
        assertEquals("tmdb:1", row.id)
        assertNull(row.rawReleaseDate)
    }
}
