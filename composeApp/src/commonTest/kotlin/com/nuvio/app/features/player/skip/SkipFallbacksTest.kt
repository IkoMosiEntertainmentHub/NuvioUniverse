package com.nuvio.app.features.player.skip

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SkipFallbacksTest {
    private fun marker(type: String, provider: String = "introdb", start: Double = 10.0, end: Double = 20.0) = SkipInterval(start, end, type, provider)

    @Test fun `complete primary markers never invoke fallback providers`() = runTest {
        val primary = listOf(marker("intro"), marker("recap"), marker("outro"))
        assertEquals(primary, completeSkipSegments(primary, { error("Unexpected SkipDB request") }, { error("Unexpected TheIntroDB request") }))
    }

    @Test fun `fallback is per missing category and never overwrites primary intro`() = runTest {
        val requested = mutableListOf<String>()
        val primary = marker("intro")
        val result = completeSkipSegments(listOf(primary), { kind ->
            requested += kind
            if (kind == "recap") SkipProviderResult(listOf(marker("recap", "skipdb"))) else SkipProviderResult()
        }, { listOf(marker("intro", "theintrodb"), marker("outro", "theintrodb")) })
        assertEquals(setOf("recap", "outro"), requested.toSet())
        assertEquals(primary, result.first { it.type == "intro" })
        assertEquals("skipdb", result.first { it.type == "recap" }.provider)
        assertEquals("theintrodb", result.first { it.type == "outro" }.provider)
    }

    @Test fun `confirmed absent markers stop further lookup for that category`() = runTest {
        val result = completeSkipSegments(listOf(marker("intro"), marker("outro")),
            { SkipProviderResult(confirmedAbsent = setOf("recap")) }, { error("Confirmed absent recap must not be replaced") })
        assertEquals(2, result.size)
    }

    @Test fun `primary confirmed absence is not mistaken for missing data`() = runTest {
        val result = completeSkipSegments(listOf(marker("intro"), marker("outro")),
            { error("Primary confirmed no recap") }, { error("Primary confirmed no recap") }, primaryAbsent = setOf("recap"))
        assertEquals(2, result.size)
        assertEquals(setOf("recap"), IntroDbSegmentsResponse(noRecap = true).confirmedAbsentCategories())
    }

    @Test fun `SkipDB rejects uncertain and invalid markers and converts milliseconds`() {
        assertEquals(1.0, parseSkipDb("""{"segments":{"intro":{"start_ms":1000,"end_ms":9000,"match":"exact"}}}""", "intro", false, 20_000).intervals.single().startTime)
        assertTrue(parseSkipDb("""{"segments":{"intro":{"start_ms":1000,"end_ms":9000,"match":"out-of-range"}}}""", "intro", false, 20_000).intervals.isEmpty())
        assertEquals(setOf("intro"), parseSkipDb("""{"segments":{"intro":{"start_ms":0,"end_ms":0}}}""", "intro", false, null).confirmedAbsent)
        assertTrue(parseSkipDb("""{"segments":{"outro":{"start_ms":1000,"end_ms":99000}}}""", "outro", true, 20_000).intervals.isEmpty())
    }

    @Test fun `TheIntroDB preserves separated credits and resolves open ends only with known duration`() {
        val body = """{"intro":[{"start_ms":null,"end_ms":20000}],"credits":[{"start_ms":80000,"end_ms":85000},{"start_ms":90000,"end_ms":null}]}"""
        val result = parseTheIntroDb(body, true, 100_000)
        assertEquals(3, result.size)
        assertEquals(0.0, result.first().startTime)
        assertEquals(100.0, result.last().endTime)
        assertEquals("movie-credits", result.last().type)
        assertEquals(2, parseTheIntroDb(body, true, null).size)
        assertEquals(2, mergeSkipProviders(result).count { it.type == "movie-credits" })
    }

    @Test fun `cancellation propagates rather than producing cached empty results`() = runTest {
        assertFailsWith<CancellationException> { completeSkipSegments(emptyList(), { throw CancellationException() }, { emptyList() }) }
    }
}
