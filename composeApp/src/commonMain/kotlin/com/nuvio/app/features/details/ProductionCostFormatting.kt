package com.nuvio.app.features.details

import kotlin.math.round

internal fun compactProductionCost(value: Double?, currency: String = "USD"): String {
    if (value == null || !value.isFinite() || value <= 0) return "Unknown"
    val (scale, suffix) = when {
        value >= 1_000_000_000 -> 1_000_000_000.0 to "B"
        value >= 1_000_000 -> 1_000_000.0 to "M"
        value >= 1_000 -> 1_000.0 to "K"
        else -> 1.0 to ""
    }
    val rounded = round(value / scale * 10) / 10
    val number = if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
    return (if (currency == "USD") "$" else "$currency ") + number + suffix
}
