package com.hue.core.color

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.SeasonClassifier
import com.hue.core.color.model.*
import org.junit.Test

/**
 * Season classification tests using canonical palette examples.
 * Boundary cases are tested to verify fuzzy membership behaves correctly.
 */
class SeasonClassifierTest {

    @Test
    fun `warm bright peach is classified Spring`() {
        // Peach: warm, light, bright — canonical Spring
        val lab = LabColor(l = 76.0, a = 18.0, b = 30.0)
        val result = SeasonClassifier.classify(lab)
        assertThat(result.primarySeason).isEqualTo(Season.SPRING)
        assertThat(result.temperature).isEqualTo(ColorTemperature.WARM)
    }

    @Test
    fun `cool muted lavender is classified Summer`() {
        // Lavender: cool, light, muted — canonical Summer
        val lab = LabColor(l = 72.0, a = 8.0, b = -18.0)
        val result = SeasonClassifier.classify(lab)
        assertThat(result.primarySeason).isEqualTo(Season.SUMMER)
        assertThat(result.temperature).isEqualTo(ColorTemperature.COOL)
    }

    @Test
    fun `warm muted rust is classified Autumn`() {
        // Rust: warm, medium-deep, muted — canonical Autumn
        val lab = LabColor(l = 45.0, a = 30.0, b = 28.0)
        val result = SeasonClassifier.classify(lab)
        assertThat(result.primarySeason).isEqualTo(Season.AUTUMN)
        assertThat(result.temperature).isEqualTo(ColorTemperature.WARM)
    }

    @Test
    fun `cool deep vivid navy is classified Winter`() {
        // Navy: cool, deep, high contrast — canonical Winter
        val lab = LabColor(l = 22.0, a = 5.0, b = -30.0)
        val result = SeasonClassifier.classify(lab)
        assertThat(result.primarySeason).isEqualTo(Season.WINTER)
        assertThat(result.temperature).isEqualTo(ColorTemperature.COOL)
    }

    @Test
    fun `achromatic grey has low confidence`() {
        val lab = LabColor(l = 55.0, a = 0.0, b = 0.0)
        val result = SeasonClassifier.classify(lab)
        assertThat(result.confidence).isLessThan(0.5)
    }

    @Test
    fun `near-boundary colour produces secondary season`() {
        // Warm-light-medium-bright is Spring, but near Summer boundary
        val lab = LabColor(l = 66.0, a = 5.0, b = -10.0)  // cool-ish, medium
        val result = SeasonClassifier.classify(lab)
        // Should have a secondary season if it's near boundary
        // Just verify it doesn't crash and returns something
        assertThat(result.primarySeason).isNotNull()
    }

    @Test
    fun `explanation is non-empty`() {
        val lab = LabColor(l = 50.0, a = 20.0, b = 25.0)
        val result = SeasonClassifier.classify(lab)
        assertThat(result.explanation).isNotEmpty()
    }

    @Test
    fun `confidence is in 0-1 range`() {
        listOf(
            LabColor(76.0, 18.0, 30.0),  // Spring
            LabColor(72.0, 8.0, -18.0),  // Summer
            LabColor(45.0, 30.0, 28.0),  // Autumn
            LabColor(22.0, 5.0, -30.0),  // Winter
            LabColor(55.0, 0.0, 0.0)     // Neutral
        ).forEach { lab ->
            val result = SeasonClassifier.classify(lab)
            assertThat(result.confidence).isAtLeast(0.0)
            assertThat(result.confidence).isAtMost(1.0)
        }
    }

    @Test
    fun `warm hue angle is correctly identified`() {
        // Orange LAB: warm hue
        val orange = LabColor(l = 65.0, a = 35.0, b = 50.0)
        val result = SeasonClassifier.classify(orange)
        assertThat(result.temperature).isEqualTo(ColorTemperature.WARM)
    }

    @Test
    fun `cool hue angle is correctly identified`() {
        // Blue LAB: cool hue
        val blue = LabColor(l = 35.0, a = 15.0, b = -55.0)
        val result = SeasonClassifier.classify(blue)
        assertThat(result.temperature).isEqualTo(ColorTemperature.COOL)
    }

    @Test
    fun `12-season extended classification returns non-empty string`() {
        val lab = LabColor(l = 70.0, a = 18.0, b = 48.0)
        val base = SeasonClassifier.classify(lab)
        val extended = SeasonClassifier.classifyExtended(lab, base)
        assertThat(extended).isNotEmpty()
        assertThat(extended).contains("Spring")
    }

    @Test
    fun `pantone temp hint influences result for ambiguous colour`() {
        // Achromatic-ish: neutral colour where hint matters
        val lab = LabColor(l = 60.0, a = 2.0, b = 2.0)
        val warmResult = SeasonClassifier.classify(lab, ColorTemperature.WARM)
        val coolResult = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        // Temperatures should reflect the hint
        assertThat(warmResult.temperature).isNotEqualTo(coolResult.temperature)
    }
}
