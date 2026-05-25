package com.hue.core.color.model

import androidx.annotation.ColorInt
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

data class RgbColor(val r: Int, val g: Int, val b: Int) {
    val hex: String get() = "#%02X%02X%02X".format(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))

    @ColorInt
    fun toColorInt(): Int = android.graphics.Color.rgb(r, g, b)

    companion object {
        fun fromColorInt(@ColorInt color: Int) = RgbColor(
            android.graphics.Color.red(color),
            android.graphics.Color.green(color),
            android.graphics.Color.blue(color)
        )

        fun fromHex(hex: String): RgbColor {
            val clean = hex.trimStart('#')
            return RgbColor(
                clean.substring(0, 2).toInt(16),
                clean.substring(2, 4).toInt(16),
                clean.substring(4, 6).toInt(16)
            )
        }
    }
}

data class LabColor(val l: Double, val a: Double, val b: Double) {
    val chroma: Double get() = sqrt(a * a + b * b)
    val hueAngle: Double get() = Math.toDegrees(atan2(b, a)).let { if (it < 0) it + 360 else it }
}

data class XyzColor(val x: Double, val y: Double, val z: Double)

data class HslColor(val h: Double, val s: Double, val l: Double)

enum class ColorTemperature { WARM, COOL, NEUTRAL }

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    val displayName: String get() = name.lowercase().replaceFirstChar { it.uppercaseChar() }
}

data class SeasonResult(
    val primarySeason: Season,
    val secondarySeason: Season?,
    val primaryWeight: Double,
    val temperature: ColorTemperature,
    val confidence: Double,
    val lightness: LightnessCategory,
    val chroma: ChromaCategory,
    val explanation: String
)

enum class LightnessCategory { LIGHT, MEDIUM, DEEP }
enum class ChromaCategory { BRIGHT, MUTED, NEUTRAL }

data class DominantColorResult(
    val rgb: RgbColor,
    val lab: LabColor,
    val clusterPixelFraction: Double,
    val regionVariance: Double
)

data class PantoneMatch(
    val code: String,
    val name: String,
    val lab: LabColor,
    val hex: String,
    val temperature: ColorTemperature,
    val seasons: List<Season>,
    val deltaE: Double
) {
    val matchQuality: MatchQuality get() = when {
        deltaE < 1.0 -> MatchQuality.EXCELLENT
        deltaE < 2.0 -> MatchQuality.VERY_GOOD
        deltaE < 5.0 -> MatchQuality.GOOD
        deltaE < 10.0 -> MatchQuality.FAIR
        else -> MatchQuality.POOR
    }
}

enum class MatchQuality(val label: String) {
    EXCELLENT("Excellent"),
    VERY_GOOD("Very Good"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor")
}
