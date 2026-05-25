package com.hue.domain.model

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.model.*
import org.junit.Test

/** Pure model / sealed-class coverage. No Android deps. */
class DomainModelTest {

    private fun makeLab(l: Double = 50.0, a: Double = 20.0, b: Double = -30.0) =
        LabColor(l, a, b)

    private fun makePantoneColor(
        code: String = "18-1550 TCX",
        name: String = "Burnt Orange",
        lab: LabColor = makeLab(),
        temperature: ColorTemperature = ColorTemperature.WARM,
        seasons: List<Season> = listOf(Season.AUTUMN)
    ) = PantoneFhiColor(code = code, name = name, lab = lab,
                        hex = "#B04020", temperature = temperature, seasons = seasons)

    // ── PantoneMatchResult match quality ──────────────────────────────────

    @Test fun `deltaE below 1 is EXCELLENT`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 0.8)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.EXCELLENT)
    }

    @Test fun `deltaE 1 to 2 is VERY_GOOD`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 1.5)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.VERY_GOOD)
    }

    @Test fun `deltaE 2 to 5 is GOOD`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 3.2)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.GOOD)
    }

    @Test fun `deltaE 5 to 10 is FAIR`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 7.0)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.FAIR)
    }

    @Test fun `deltaE above 10 is POOR`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 12.0)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.POOR)
    }

    @Test fun `deltaE exactly 1 is VERY_GOOD`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 1.0)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.VERY_GOOD)
    }

    @Test fun `deltaE exactly 2 is GOOD`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 2.0)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.GOOD)
    }

    @Test fun `deltaE exactly 5 is FAIR`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 5.0)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.FAIR)
    }

    @Test fun `deltaE exactly 10 is FAIR`() {
        val match = PantoneMatchResult(makePantoneColor(), deltaE = 10.0)
        assertThat(match.matchQuality).isEqualTo(MatchQuality.FAIR)
    }

    // ── AnalysisError sealed class ────────────────────────────────────────

    @Test fun `AnalysisError subclasses are distinct`() {
        val errors: List<AnalysisError> = listOf(
            AnalysisError.LowLight,
            AnalysisError.Overexposed,
            AnalysisError.TooNoisy,
            AnalysisError.AchromaticFabric,
            AnalysisError.NoPantoneMatch,
            AnalysisError.Unknown("something went wrong")
        )
        assertThat(errors.map { it::class }.distinct().size).isEqualTo(errors.size)
    }

    @Test fun `Unknown error preserves message`() {
        val err = AnalysisError.Unknown("test message")
        assertThat(err.message).isEqualTo("test message")
    }

    // ── AnalysisState sealed class ─────────────────────────────────────────

    @Test fun `Idle state is distinct from Loading`() {
        assertThat(AnalysisState.Idle).isNotEqualTo(AnalysisState.Extracting)
    }

    @Test fun `all AnalysisState phases are distinct`() {
        val states: List<AnalysisState> = listOf(
            AnalysisState.Idle,
            AnalysisState.Extracting,
            AnalysisState.Matching,
            AnalysisState.Classifying
        )
        assertThat(states.map { it::class }.distinct().size).isEqualTo(states.size)
    }

    @Test fun `FabricAnalysis default id is 0`() {
        val analysis = FabricAnalysis(
            timestamp = System.currentTimeMillis(),
            thumbnailPath = "/path/to/file",
            dominantRgb = RgbColor(180, 90, 45),
            dominantLab = LabColor(52.0, 20.0, 30.0),
            topMatches = emptyList(),
            season = SeasonResult(
                primarySeason = Season.AUTUMN,
                secondarySeason = null,
                primaryWeight = 0.8,
                temperature = ColorTemperature.WARM,
                confidence = 0.8,
                lightness = LightnessCategory.MEDIUM,
                chroma = ChromaCategory.MUTED,
                explanation = "Test explanation"
            ),
            regionVariance = 50.0
        )
        assertThat(analysis.id).isEqualTo(0L)
    }

    // ── RgbColor ──────────────────────────────────────────────────────────

    @Test fun `RgbColor clamps hex output for out-of-range values`() {
        val rgb = RgbColor(300, -5, 128)  // out of range
        val hex = rgb.hex
        assertThat(hex).startsWith("#")
        assertThat(hex).hasLength(7)
    }

    @Test fun `RgbColor fromColorInt and toColorInt round-trip`() {
        val original = RgbColor(123, 45, 200)
        val colorInt = original.toColorInt()
        val recovered = RgbColor.fromColorInt(colorInt)
        assertThat(recovered).isEqualTo(original)
    }

    // ── MatchQuality labels ────────────────────────────────────────────────

    @Test fun `all MatchQuality labels are non-empty`() {
        MatchQuality.values().forEach { q ->
            assertThat(q.label).isNotEmpty()
        }
    }
}
