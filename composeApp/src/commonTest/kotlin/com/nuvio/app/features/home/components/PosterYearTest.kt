package com.nuvio.app.features.home.components

import kotlin.test.*

class PosterYearTest {
    @Test fun `landscape labels show a known release year without inventing one`() {
        assertEquals("2022", posterYear("2022–", null))
        assertEquals("2012", posterYear(null, "2012-12-31"))
        assertNull(posterYear("TBA", null))
        assertNull(posterYear(null, null))
    }
}
