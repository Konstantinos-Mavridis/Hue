package com.hue.domain.model

import com.hue.core.color.model.*

data class PantoneFhiColor(
    val id: Long = 0,
    val code: String,
    val name: String,
    val lab: LabColor,
    val hex: String,
    val temperature: ColorTemperature,
    val seasons: List<Season>
)

data class PantoneMatchResult(
    val color: PantoneFhiColor,
    val deltaE: Double
) {
    val matchQuality: MatchQuality get() = when {
        deltaE < 1.0  -> MatchQuality.EXCELLENT
        deltaE < 2.0  -> MatchQuality.VERY_GOOD
        deltaE < 5.0  -> MatchQuality.GOOD
        deltaE < 10.0 -> MatchQuality.FAIR
        else          -> MatchQuality.POOR
    }
}

data class FabricAnalysis(
    val id: Long = 0,
    val timestamp: Long,
    val thumbnailPath: String,
    val dominantRgb: RgbColor,
    val dominantLab: LabColor,
    val topMatches: List<PantoneMatchResult>,
    val season: SeasonResult,
    val regionVariance: Double,
    val notes: String = ""
)

data class AnalysisInput(
    val croppedBitmapPath: String,
    val exifIlluminantKey: String? = null
)

sealed class AnalysisError {
    object LowLight : AnalysisError()
    object Overexposed : AnalysisError()
    object TooNoisy : AnalysisError()
    object AchromaticFabric : AnalysisError()
    object NoPantoneMatch : AnalysisError()
    data class Unknown(val message: String) : AnalysisError()
}

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Extracting : AnalysisState()
    object Matching : AnalysisState()
    object Classifying : AnalysisState()
    data class Success(val result: FabricAnalysis) : AnalysisState()
    data class Warning(val result: FabricAnalysis, val warning: AnalysisError) : AnalysisState()
    data class Error(val error: AnalysisError) : AnalysisState()
}
