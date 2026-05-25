package com.hue.data.pantone.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.hue.core.color.model.*
import com.hue.domain.model.*

@Entity(
    tableName = "scan_history",
    indices = [Index("timestamp")]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val thumbnailPath: String,
    val dominantR: Int,
    val dominantG: Int,
    val dominantB: Int,
    val dominantLabL: Double,
    val dominantLabA: Double,
    val dominantLabB: Double,
    val topMatchesJson: String,    // JSON array of PantoneMatchDto
    val primarySeason: String,
    val secondarySeason: String?,
    val temperature: String,
    val confidence: Double,
    val lightness: String,
    val chroma: String,
    val explanation: String,
    val regionVariance: Double,
    val notes: String = ""
) {
    fun toDomain(gson: Gson): FabricAnalysis {
        val matchType = object : TypeToken<List<PantoneMatchDto>>() {}.type
        val matchDtos: List<PantoneMatchDto> = gson.fromJson(topMatchesJson, matchType) ?: emptyList()
        return FabricAnalysis(
            id = id,
            timestamp = timestamp,
            thumbnailPath = thumbnailPath,
            dominantRgb = RgbColor(dominantR, dominantG, dominantB),
            dominantLab = LabColor(dominantLabL, dominantLabA, dominantLabB),
            topMatches = matchDtos.map { it.toMatchResult() },
            season = SeasonResult(
                primarySeason = Season.valueOf(primarySeason),
                secondarySeason = secondarySeason?.let { Season.valueOf(it) },
                primaryWeight = confidence,
                temperature = ColorTemperature.valueOf(temperature),
                confidence = confidence,
                lightness = LightnessCategory.valueOf(lightness),
                chroma = ChromaCategory.valueOf(chroma),
                explanation = explanation
            ),
            regionVariance = regionVariance,
            notes = notes
        )
    }

    companion object {
        fun fromDomain(analysis: FabricAnalysis, gson: Gson) = ScanHistoryEntity(
            id = analysis.id,
            timestamp = analysis.timestamp,
            thumbnailPath = analysis.thumbnailPath,
            dominantR = analysis.dominantRgb.r,
            dominantG = analysis.dominantRgb.g,
            dominantB = analysis.dominantRgb.b,
            dominantLabL = analysis.dominantLab.l,
            dominantLabA = analysis.dominantLab.a,
            dominantLabB = analysis.dominantLab.b,
            topMatchesJson = gson.toJson(analysis.topMatches.map { PantoneMatchDto.from(it) }),
            primarySeason = analysis.season.primarySeason.name,
            secondarySeason = analysis.season.secondarySeason?.name,
            temperature = analysis.season.temperature.name,
            confidence = analysis.season.confidence,
            lightness = analysis.season.lightness.name,
            chroma = analysis.season.chroma.name,
            explanation = analysis.season.explanation,
            regionVariance = analysis.regionVariance,
            notes = analysis.notes
        )
    }
}

data class PantoneMatchDto(
    val code: String,
    val name: String,
    val labL: Double, val labA: Double, val labB: Double,
    val hex: String,
    val temperature: String,
    val seasons: String,
    val deltaE: Double
) {
    fun toMatchResult() = PantoneMatchResult(
        color = com.hue.domain.model.PantoneFhiColor(
            code = code, name = name,
            lab = LabColor(labL, labA, labB),
            hex = hex,
            temperature = ColorTemperature.valueOf(temperature),
            seasons = seasons.split(",").map { Season.valueOf(it.trim()) }
        ),
        deltaE = deltaE
    )

    companion object {
        fun from(match: PantoneMatchResult) = PantoneMatchDto(
            code = match.color.code,
            name = match.color.name,
            labL = match.color.lab.l,
            labA = match.color.lab.a,
            labB = match.color.lab.b,
            hex = match.color.hex,
            temperature = match.color.temperature.name,
            seasons = match.color.seasons.joinToString(",") { it.name },
            deltaE = match.deltaE
        )
    }
}
