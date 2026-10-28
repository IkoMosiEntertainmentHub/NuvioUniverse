package com.nuvio.app.features.updater

internal data class SemanticVersion(
    val major: Long,
    val minor: Long,
    val patch: Long,
    val prerelease: List<String>
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }

        if (prerelease.isEmpty() && other.prerelease.isEmpty()) return 0
        if (prerelease.isEmpty()) return 1
        if (other.prerelease.isEmpty()) return -1

        val sharedSize = minOf(prerelease.size, other.prerelease.size)
        for (index in 0 until sharedSize) {
            comparePrereleaseIdentifier(
                prerelease[index],
                other.prerelease[index]
            ).takeIf { it != 0 }?.let { return it }
        }
        return compareValues(prerelease.size, other.prerelease.size)
    }

    private fun comparePrereleaseIdentifier(left: String, right: String): Int {
        val leftNumeric = left.all(Char::isDigit)
        val rightNumeric = right.all(Char::isDigit)

        if (leftNumeric && rightNumeric) {
            val normalizedLeft = left.trimStart('0').ifEmpty { "0" }
            val normalizedRight = right.trimStart('0').ifEmpty { "0" }
            compareValues(normalizedLeft.length, normalizedRight.length)
                .takeIf { it != 0 }
                ?.let { return it }
            return normalizedLeft.compareTo(normalizedRight)
        }
        if (leftNumeric) return -1
        if (rightNumeric) return 1
        return left.compareTo(right)
    }
}

internal object VersionUtils {
    private val versionPattern = Regex(
        "^(\\d+)\\.(\\d+)\\.(\\d+)" +
            "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?" +
            "(?:\\+[0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*)?$"
    )

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.trim().removePrefix("v").removePrefix("V")
    }

    fun isUniverseIdentifier(raw: String?): Boolean = raw?.trim()?.startsWith("NuvioUniverse-") == true

    fun parse(raw: String?): SemanticVersion? {
        val universe = Regex("^NuvioUniverse-(.+)-\\((\\d+)\\)-v(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(normalize(raw))
        if (universe != null) return SemanticVersion(
            universe.groupValues[3].toLongOrNull() ?: return null,
            universe.groupValues[4].toLongOrNull() ?: return null,
            universe.groupValues[5].toLongOrNull() ?: return null,
            emptyList(),
        )
        // Keep the friendly fork label, but compare its version components numerically.
        val comparable = normalize(raw).replace("-beta-NuvioUniverse-", "-beta.NuvioUniverse.")
        val match = versionPattern.matchEntire(comparable) ?: return null
        return SemanticVersion(
            major = match.groupValues[1].toLongOrNull() ?: return null,
            minor = match.groupValues[2].toLongOrNull() ?: return null,
            patch = match.groupValues[3].toLongOrNull() ?: return null,
            prerelease = match.groupValues[4]
                .takeIf(String::isNotEmpty)
                ?.split('.')
                .orEmpty()
        )
    }

    fun isPrerelease(raw: String?): Boolean = parse(raw)?.prerelease?.isNotEmpty() == true

    fun isRemoteNewer(remote: String?, local: String?): Boolean {
        val remoteVersion = parse(remote) ?: return false
        val localVersion = parse(local) ?: return false
        if (isUniverseIdentifier(remote) && isUniverseIdentifier(local) && remoteVersion == localVersion) {
            fun build(value: String?): Long = Regex("-\\((\\d+)\\)-v").find(value.orEmpty())?.groupValues?.get(1)?.toLongOrNull() ?: -1
            return build(remote) > build(local)
        }
        return remoteVersion > localVersion
    }
}
