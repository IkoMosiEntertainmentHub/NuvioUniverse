package com.nuvio.app.features.player

import io.ktor.http.Url

/** Details metadata is canonical; shelf artwork must not leak into player/media surfaces. */
internal fun defaultPlayerArtwork(metadataUrl: String?, launchUrl: String?): String? =
    metadataUrl?.takeIf { it.isNotBlank() } ?: launchUrl?.takeIf { url ->
        url.startsWith("file:") || url.startsWith("content:") ||
            runCatching { Url(url).host.lowercase() }.getOrNull() in setOf(
                "image.tmdb.org", "images.metahub.space", "m.media-amazon.com",
                "ia.media-imdb.com", "artworks.thetvdb.com", "assets.fanart.tv",
            )
    }
