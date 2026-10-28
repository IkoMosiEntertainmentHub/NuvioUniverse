package com.nuvio.app.features.details

import android.app.Application
import android.content.Context
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UniverseSettingsTest {
    @BeforeTest fun initialize() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("nuvio_meta_screen_settings", Context.MODE_PRIVATE).edit().clear().commit()
        MetaScreenSettingsStorage.initialize(context)
        MetaScreenSettingsRepository.clearLocalState()
        MetaScreenSettingsRepository.ensureLoaded()
    }

    @AfterTest fun clear() { MetaScreenSettingsRepository.clearLocalState() }

    @Test fun defaultIsOldestFirstWithLandscapePosters() {
        val settings = MetaScreenSettingsRepository.uiState.value
        assertEquals(UniverseSortOrder.AiredOldest, settings.universeSortOrder)
        assertEquals(UniversePosterStyle.Landscape, settings.universePosterStyle)
        assertEquals("Universe", settings.items.first { it.key == MetaScreenSectionKey.COLLECTION }.title)
    }

    @Test fun choicesRoundTripThroughTheAccountProfileSettingsPayload() {
        MetaScreenSettingsRepository.setUniverseSortOrder(UniverseSortOrder.MostPopular)
        MetaScreenSettingsRepository.setUniversePosterStyle(UniversePosterStyle.Posters)
        val exported = MetaScreenSettingsStorage.loadPayload()!!
        MetaScreenSettingsRepository.resetToDefaults()
        MetaScreenSettingsStorage.savePayload(exported)
        MetaScreenSettingsRepository.onProfileChanged()
        assertEquals(UniverseSortOrder.MostPopular, MetaScreenSettingsRepository.uiState.value.universeSortOrder)
        assertEquals(UniversePosterStyle.Posters, MetaScreenSettingsRepository.uiState.value.universePosterStyle)
    }

    @Test fun olderAndUnknownSettingsKeepSafeDefaults() {
        MetaScreenSettingsStorage.savePayload("""{"universeSortOrder":"future","universePosterStyle":"future","blur_unwatched_episodes":true}""")
        MetaScreenSettingsRepository.onProfileChanged()
        val settings = MetaScreenSettingsRepository.uiState.value
        assertEquals(UniverseSortOrder.AiredOldest, settings.universeSortOrder)
        assertEquals(UniversePosterStyle.Landscape, settings.universePosterStyle)
        assertTrue(settings.blurUnwatchedEpisodes)
    }

    @Test fun resetRestoresDefaultsWithoutChangingGlobalPosterStyle() {
        MetaScreenSettingsRepository.setUniverseSortOrder(UniverseSortOrder.AiredNewest)
        MetaScreenSettingsRepository.setUniversePosterStyle(UniversePosterStyle.Posters)
        MetaScreenSettingsRepository.resetToDefaults()
        MetaScreenSettingsRepository.onProfileChanged()
        assertEquals(UniverseSortOrder.AiredOldest, MetaScreenSettingsRepository.uiState.value.universeSortOrder)
        assertEquals(UniversePosterStyle.Landscape, MetaScreenSettingsRepository.uiState.value.universePosterStyle)
    }
}
