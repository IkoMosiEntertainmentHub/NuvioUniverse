package com.nuvio.app.features.details

import kotlin.test.*

class ProductionCostFormattingTest {
    @Test fun `production costs use compact readable units`() {
        assertEquals("$100M", compactProductionCost(100_000_000.0))
        assertEquals("$127M", compactProductionCost(127_000_000.0))
        assertEquals("$1.3B", compactProductionCost(1_270_000_000.0))
        assertEquals("EUR 250K", compactProductionCost(250_000.0, "EUR"))
        assertEquals("Unknown", compactProductionCost(null))
        assertEquals("Unknown", compactProductionCost(0.0))
    }
}
