package com.nuvio.app.features.updater

import kotlin.test.*

class UniverseVersionTest {
    @Test fun `upstream beta does not classify personal release as prerelease`() {
        val label = "NuvioUniverse-0.6.5-beta-(148)-v1.2.4"
        assertFalse(VersionUtils.isPrerelease(label))
        assertEquals(1, ReleaseSelector.eligibleReleases(listOf(GitHubReleaseDto(tagName = label, name = label)), UpdateChannel.STABLE).size)
        assertTrue(ReleaseSelector.eligibleReleases(listOf(GitHubReleaseDto(tagName = label, prerelease = true)), UpdateChannel.STABLE).isEmpty())
    }

    @Test fun `personal increments and official update compare independently`() {
        assertTrue(VersionUtils.isRemoteNewer("NuvioUniverse-0.6.5-beta-(148)-v1.2.5", "NuvioUniverse-0.6.5-beta-(148)-v1.2.4"))
        assertTrue(VersionUtils.isRemoteNewer("NuvioUniverse-0.6.6-(149)-v1.2.4", "NuvioUniverse-0.6.5-beta-(148)-v1.2.4"))
        assertFalse(VersionUtils.isRemoteNewer("NuvioUniverse-0.6.6-(149)-v1.2.3", "NuvioUniverse-0.6.5-beta-(148)-v1.2.4"))
    }
}
