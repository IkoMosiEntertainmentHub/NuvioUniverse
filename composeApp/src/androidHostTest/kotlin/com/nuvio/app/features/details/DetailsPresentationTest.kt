package com.nuvio.app.features.details

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.features.details.components.DetailAdditionalInfoSection
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.home.components.HomePosterCard
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.math.abs
import kotlin.test.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w390dp-h844dp")
class DetailsPresentationTest {
    @get:Rule val compose = createComposeRule()

    @BeforeTest fun defaults() {
        MetaScreenSettingsStorage.initialize(RuntimeEnvironment.getApplication())
        MetaScreenSettingsRepository.clearLocalState()
        MetaScreenSettingsRepository.resetToDefaults()
        PosterCardStyleRepository.resetToDefaults()
    }

    @AfterTest fun clear() { MetaScreenSettingsRepository.clearLocalState() }

    @Test fun relationshipIsAboveTitleAndLandscapeYearIsBelowTitle() {
        compose.setContent {
            NuvioTheme {
                HomePosterCard(MetaPreview("tt1", "movie", "Card title", releaseInfo = "2026"),
                    landscapeOverride = true, showLandscapeOverlay = false, labelAboveTitle = "Spin-off")
            }
        }
        val relation = compose.onNodeWithText("Spin-off").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        // The empty-artwork placeholder also contains the title; inspect the label below it.
        val title = compose.onAllNodesWithText("Card title")[1].assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val year = compose.onNodeWithText("2026").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(relation.top < title.top)
        assertTrue(title.top < year.top)
    }

    @Test fun compactInformationUsesTwoColumnsAndFinancialsStartsCollapsed() {
        compose.setContent {
            NuvioTheme {
                DetailAdditionalInfoSection(MetaDetails("tt1", "movie", "Movie", status = "Released", releaseInfo = "2026",
                    runtime = "120", ageRating = "PG", country = "US", language = "en"))
            }
        }
        val status = compose.onNodeWithText("Released").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val year = compose.onNodeWithText("2026").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(year.left > status.left)
        assertTrue(abs(year.top - status.top) < 2f)
        compose.onNodeWithText("Financials ▾").assertIsDisplayed()
        compose.onNodeWithText("Box office history").assertDoesNotExist()
    }
}
