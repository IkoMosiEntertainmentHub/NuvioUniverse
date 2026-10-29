package com.nuvio.app.features.details

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.*

class FinancialsTest {
    @Test fun `archive values and dated timelines are parsed without mixing current snapshots`() {
        val result = parseArchiveFinancials(Json.parseToJsonElement("""{
            "id":"film","budget":100000000,"worldwide":250000000,"domestic":100000000,"foreign":150000000,
            "current":{"worldwide":900000000},"opening":{"gross":10000000},
            "daily":[{"date":"2000-01-02","gross":2,"toDate":3},{"date":"2000-01-01","gross":1}],
            "weeks":[{"start":"2000-01-01","gross":5}],"sources":[{"captured":"20191017"}]
        }""").jsonObject)
        assertEquals(250000000L, result.worldwide)
        assertEquals(150000000L, result.grossMinusBudget)
        assertEquals(2.5, result.budgetMultiple)
        assertEquals("2000-01-01", result.daily.first().date)
        assertNull(result.daily.first().cumulative)
        assertEquals("20191017", result.captured)
        assertEquals(1, result.weekly.size)
    }

    @Test fun `missing budgets and zero placeholders stay unknown and never become profits`() {
        val missing = parseArchiveFinancials(Json.parseToJsonElement("""{"id":"film","budget":0,"worldwide":null}""").jsonObject)
        assertNull(missing.budget)
        assertNull(missing.worldwide)
        assertNull(missing.grossMinusBudget)
        assertNull(missing.budgetMultiple)
    }

    @Test fun `episode subtotals reject mixed currencies duplicate claims and aggregate double counting`() {
        val one = ProductionCost("Episode 1", "Episode", 10.0, "USD", null, null)
        val two = one.copy(title = "Episode 2", amount = 20.0)
        assertEquals(30.0 to "USD", knownProductionCostTotal(listOf(one, two)))
        assertNull(knownProductionCostTotal(listOf(one, two.copy(currency = "EUR"))))
        assertNull(knownProductionCostTotal(listOf(one, one.copy(amount = 11.0))))
        assertNull(knownProductionCostTotal(listOf(one, two.copy(level = "Season"))))
    }
}
