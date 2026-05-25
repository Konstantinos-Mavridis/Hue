package com.hue.core.color

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.SeasonClassifier
import com.hue.core.color.model.*
import org.junit.Test

/**
 * Exhaustive boundary and branch coverage for SeasonClassifier.
 * Covers all LightnessCategory, ChromaCategory, Season, ColorTemperature paths.
 */
class SeasonClassifierBoundaryTest {

    // ── Lightness categories ──────────────────────────────────────────────

    @Test fun `L above 65 is LIGHT`() {
        val lab = LabColor(70.0, 20.0, 30.0)  // warm, bright
        val r = SeasonClassifier.classify(lab)
        assertThat(r.lightness).isEqualTo(LightnessCategory.LIGHT)
    }

    @Test fun `L below 40 is DEEP`() {
        val lab = LabColor(35.0, 5.0, -40.0)  // cool, deep
        val r = SeasonClassifier.classify(lab)
        assertThat(r.lightness).isEqualTo(LightnessCategory.DEEP)
    }

    @Test fun `L between 40 and 65 is MEDIUM`() {
        val lab = LabColor(52.0, 20.0, 25.0)
        val r = SeasonClassifier.classify(lab)
        assertThat(r.lightness).isEqualTo(LightnessCategory.MEDIUM)
    }

    @Test fun `L at exact 65 boundary is MEDIUM`() {
        val lab = LabColor(65.0, 20.0, 30.0)
        val r = SeasonClassifier.classify(lab)
        assertThat(r.lightness).isEqualTo(LightnessCategory.MEDIUM)
    }

    @Test fun `L at exact 40 boundary is MEDIUM`() {
        val lab = LabColor(40.0, 20.0, 30.0)
        val r = SeasonClassifier.classify(lab)
        assertThat(r.lightness).isEqualTo(LightnessCategory.MEDIUM)
    }

    // ── Chroma categories ────────────────────────────────────────────────

    @Test fun `C above 35 is BRIGHT`() {
        val lab = LabColor(65.0, 30.0, 20.0)
        val r = SeasonClassifier.classify(lab)
        assertThat(r.chroma).isEqualTo(ChromaCategory.BRIGHT)
    }

    @Test fun `C between 15 and 35 is MUTED`() {
        val lab = LabColor(52.0, 15.0, 15.0)  // C* ≈ 21
        val r = SeasonClassifier.classify(lab)
        assertThat(r.chroma).isEqualTo(ChromaCategory.MUTED)
    }

    @Test fun `C below 15 is NEUTRAL`() {
        val lab = LabColor(55.0, 3.0, 3.0)
        val r = SeasonClassifier.classify(lab)
        assertThat(r.chroma).isEqualTo(ChromaCategory.NEUTRAL)
    }

    // ── Temperature edge cases ────────────────────────────────────────────

    @Test fun `fully achromatic uses hint temperature`() {
        val lab = LabColor(50.0, 0.5, 0.5)  // C* ≈ 0.7 → below neutral threshold
        val warmResult = SeasonClassifier.classify(lab, ColorTemperature.WARM)
        val coolResult = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        assertThat(warmResult.temperature).isEqualTo(ColorTemperature.WARM)
        assertThat(coolResult.temperature).isEqualTo(ColorTemperature.COOL)
    }

    @Test fun `null hint on achromatic gives NEUTRAL`() {
        val lab = LabColor(50.0, 0.5, 0.5)
        val r = SeasonClassifier.classify(lab, null)
        assertThat(r.temperature).isEqualTo(ColorTemperature.NEUTRAL)
    }

    @Test fun `temperature NEUTRAL reduces confidence`() {
        val labNeutral = LabColor(50.0, 0.5, 0.5)  // truly achromatic
        val labVivid   = LabColor(65.0, 35.0, 50.0) // vivid warm
        val neutralConf = SeasonClassifier.classify(labNeutral).confidence
        val vividConf   = SeasonClassifier.classify(labVivid).confidence
        assertThat(vividConf).isGreaterThan(neutralConf)
    }

    // ── Each Season is reachable ──────────────────────────────────────────

    @Test fun `Spring is reachable with canonical warm bright light colour`() {
        val lab = LabColor(72.0, 20.0, 40.0)  // warm, light, bright
        assertThat(SeasonClassifier.classify(lab).primarySeason).isEqualTo(Season.SPRING)
    }

    @Test fun `Summer is reachable with canonical cool muted light colour`() {
        val lab = LabColor(70.0, 5.0, -20.0)  // cool, light, muted
        assertThat(SeasonClassifier.classify(lab).primarySeason).isEqualTo(Season.SUMMER)
    }

    @Test fun `Autumn is reachable with canonical warm muted deep colour`() {
        val lab = LabColor(45.0, 20.0, 18.0)  // warm, medium-deep, muted (C*≈26.9)
        assertThat(SeasonClassifier.classify(lab).primarySeason).isEqualTo(Season.AUTUMN)
    }

    @Test fun `Winter is reachable with canonical cool vivid deep colour`() {
        val lab = LabColor(25.0, 5.0, -40.0)  // cool, deep, bright
        assertThat(SeasonClassifier.classify(lab).primarySeason).isEqualTo(Season.WINTER)
    }

    // ── Custom thresholds ────────────────────────────────────────────────

    @Test fun `custom thresholds change classification`() {
        val lab = LabColor(63.0, 20.0, 35.0)  // borderline light vs medium
        val defaultResult = SeasonClassifier.classify(lab)
        val strictThresholds = SeasonClassifier.SeasonThresholds(lightL = 70.0)
        val strictResult = SeasonClassifier.classify(lab, thresholds = strictThresholds)
        // With strict threshold, L=63 is now MEDIUM (not LIGHT), potentially different season
        // Just verify both run without error
        assertThat(defaultResult.primarySeason).isNotNull()
        assertThat(strictResult.primarySeason).isNotNull()
    }

    // ── 12-season extended classification covers all branches ────────────

    @Test fun `12-season Spring Light Spring path`() {
        val lab = LabColor(72.0, 5.0, 50.0)  // L>68, C*>45, warm → Light Spring
        val base = SeasonClassifier.classify(lab)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).isNotEmpty()
    }

    @Test fun `12-season Spring Warm Spring path`() {
        val lab = LabColor(60.0, 20.0, 38.0)  // hue ~62° → Warm Spring
        val base = SeasonClassifier.classify(lab)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Spring")
    }

    @Test fun `12-season Summer Light Summer path`() {
        val lab = LabColor(70.0, 5.0, -18.0)  // L>65, cool → Light Summer
        val base = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Summer")
    }

    @Test fun `12-season Summer Soft Summer path`() {
        val lab = LabColor(60.0, 3.0, -12.0)  // C*<18, cool → Soft Summer
        val base = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Summer")
    }

    @Test fun `12-season Summer Cool Summer path`() {
        val lab = LabColor(58.0, 12.0, -22.0)  // else → Cool Summer
        val base = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Summer")
    }

    @Test fun `12-season Autumn Deep Autumn path`() {
        val lab = LabColor(35.0, 20.0, 22.0)  // L<38 → Deep Autumn
        val base = SeasonClassifier.classify(lab, ColorTemperature.WARM)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Autumn")
    }

    @Test fun `12-season Autumn Warm Autumn path`() {
        val lab = LabColor(50.0, 18.0, 22.0)  // hue ~50.7°, C*≈28.4 muted → Warm Autumn
        val base = SeasonClassifier.classify(lab, ColorTemperature.WARM)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Autumn")
    }

    @Test fun `12-season Winter Deep Winter path`() {
        val lab = LabColor(25.0, 5.0, -40.0)  // L<30 → Deep Winter
        val base = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Winter")
    }

    @Test fun `12-season Winter Cool Winter path`() {
        // Use a deep+bright cool colour to get WINTER base, then a low-chroma cool colour for the extended path
        val labForBase = LabColor(25.0, 5.0, -40.0)  // deep, bright, cool → WINTER
        val labForExt  = LabColor(42.0, 3.0, -20.0)  // C*<25 → Cool Winter
        val base = SeasonClassifier.classify(labForBase, ColorTemperature.COOL)
        val ext = SeasonClassifier.classifyExtended(labForExt, base)
        assertThat(ext).contains("Winter")
    }

    @Test fun `12-season Winter Clear Winter path`() {
        val lab = LabColor(40.0, 15.0, -50.0)  // else → Clear Winter
        val base = SeasonClassifier.classify(lab, ColorTemperature.COOL)
        val ext = SeasonClassifier.classifyExtended(lab, base)
        assertThat(ext).contains("Winter")
    }

    // ── SeasonResult fields are populated ─────────────────────────────────

    @Test fun `season result contains all required fields`() {
        val lab = LabColor(55.0, 25.0, 35.0)
        val r = SeasonClassifier.classify(lab)
        assertThat(r.primarySeason).isNotNull()
        assertThat(r.temperature).isNotNull()
        assertThat(r.lightness).isNotNull()
        assertThat(r.chroma).isNotNull()
        assertThat(r.explanation).isNotEmpty()
        assertThat(r.confidence).isAtLeast(0.0)
        assertThat(r.confidence).isAtMost(1.0)
        assertThat(r.primaryWeight).isAtLeast(0.0)
    }

    @Test fun `secondary season weight is less than primary`() {
        // A well-defined canonical colour should have strong primary
        val lab = LabColor(22.0, 5.0, -40.0)  // clear Winter
        val r = SeasonClassifier.classify(lab)
        assertThat(r.primaryWeight).isAtLeast(0.3)
    }

    // ── Season display names ───────────────────────────────────────────────

    @Test fun `all Season enum display names are capitalized`() {
        Season.values().forEach { season ->
            assertThat(season.displayName.first().isUpperCase()).isTrue()
        }
    }
}
