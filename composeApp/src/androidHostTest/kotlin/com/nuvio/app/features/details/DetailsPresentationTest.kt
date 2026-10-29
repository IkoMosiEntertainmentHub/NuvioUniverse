package com.nuvio.app.features.details

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.nuvio.app.core.ui.NuvioTheme
import com.nuvio.app.core.ui.PosterCardStyleRepository
import com.nuvio.app.features.details.components.DetailAdditionalInfoSection
import com.nuvio.app.features.details.components.FinancialsSection
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

    @Test fun compactInformationUsesTwoColumnsAndBoxOfficeStartsCollapsed() {
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
        compose.onNodeWithText("Box Office ▾").assertIsDisplayed()
        compose.onNodeWithText("Box office history").assertDoesNotExist()
        assertWhiteText("Released")
        assertWhiteText("2026")
        assertWhiteText("Box Office ▾")
    }

    @Test fun expandedBoxOfficeUsesWhiteTextEvenWithLightMaterialDefaults() {
        compose.setContent {
            NuvioTheme {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    FinancialsSection(MetaDetails("tt1", "movie", "Movie"), loadFinancials = {
                        Financials(budget = 100_000_000, worldwide = 250_000_000, source = "Reported figures", sourceUrl = "https://example.org",
                            daily = listOf(GrossPoint("2020-01-01", 250, 250)))
                    })
                }
            }
        }
        compose.onNodeWithText("Box Office ▾").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Production cost: $100M").assertIsDisplayed()
        assertWhiteText("Production cost: $100M")
        compose.onNodeWithText("Worldwide gross: $250M").assertDoesNotExist()
        assertWhiteText("Box Office ▴")
        compose.onNodeWithText("Financials ▾").assertDoesNotExist()
        compose.onNodeWithText("Box office history").assertDoesNotExist()
        compose.onNodeWithText("View data source").assertDoesNotExist()
        compose.onNodeWithText("Gross minus production budget", substring = true).assertDoesNotExist()

    }

    @Test fun normalDetailsStayWhiteEvenWithLightMaterialDefaults() {
        MetaScreenSettingsRepository.setCompactDetails(false)
        compose.setContent {
            NuvioTheme {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    DetailAdditionalInfoSection(MetaDetails("tt1", "movie", "Movie", status = "Released", releaseInfo = "2026"))
                }
            }
        }
        assertWhiteText("Released")
        assertWhiteText("2026")
    }

    private fun assertWhiteText(text: String) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(Color.White, layouts.single().layoutInput.style.color)
    }
}
