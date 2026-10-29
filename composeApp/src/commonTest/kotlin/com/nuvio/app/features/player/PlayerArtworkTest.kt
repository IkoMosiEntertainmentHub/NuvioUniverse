package com.nuvio.app.features.player

import kotlin.test.*

class PlayerArtworkTest {
    @Test fun `canonical metadata replaces shelf artwork and custom launch artwork is suppressed`() {
        val custom = "https://custom.example/poster/tt1.jpg"
        val backdrop = "https://image.tmdb.org/t/p/original/backdrop.jpg"
        assertEquals(backdrop, defaultPlayerArtwork(backdrop, custom))
        assertNull(defaultPlayerArtwork(null, custom))
        assertEquals(backdrop, defaultPlayerArtwork(null, backdrop))
        assertNull(defaultPlayerArtwork(null, "https://image.tmdb.org.attacker.example/image.jpg"))
        assertEquals("file:///artwork.jpg", defaultPlayerArtwork(null, "file:///artwork.jpg"))
    }
}
