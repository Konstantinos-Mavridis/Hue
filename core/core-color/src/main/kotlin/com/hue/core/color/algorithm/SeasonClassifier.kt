package com.hue.core.color.algorithm

import com.hue.core.color.model.*
import kotlin.math.*

/**
 * Colour season classifier based on the 4-season (and optionally 12-season) model.
 *
 * Algorithm uses cylindrical CIELAB (LCH) coordinates:
 *   L*  — lightness
 *   C*  — chroma (saturation)
 *   h°  — hue angle
 *
 * Thresholds are externally configurable via [SeasonThresholds] so they can be
 * tuned without code changes (remote config / A-B testing friendly).
 */
object SeasonClassifier {

    data class SeasonThresholds(
        val lightL: Double  = 65.0,
        val deepL: Double   = 40.0,
        val brightC: Double = 35.0,
        val mutedC: Double  = 15.0,
        val warmHueMin: Double = 15.0,    // yellow-orange-red arc start
        val warmHueMax: Double = 75.0,    // warm hue arc end
        val coolHueMin: Double = 165.0,   // blue-green-violet start
        val coolHueMax: Double = 300.0,   // cool hue arc end
        val neutralChromaThreshold: Double = 15.0
    )

    private val defaultThresholds = SeasonThresholds()

    fun classify(
        lab: LabColor,
        pantoneTempHint: ColorTemperature? = null,
        thresholds: SeasonThresholds = defaultThresholds
    ): SeasonResult {
        val lightness  = classifyLightness(lab.l, thresholds)
        val chroma     = classifyChroma(lab.chroma, thresholds)
        val temperature = inferTemperature(lab, pantoneTempHint, thresholds)

        // Fuzzy scores per season: higher is better (each component contributes 0–1)
        val scores = computeSeasonScores(lab, lightness, chroma, temperature, thresholds)

        val sorted = scores.entries.sortedByDescending { it.value }
        val primary   = sorted[0].key
        val secondary = sorted.getOrNull(1)?.takeIf { it.value / sorted[0].value > 0.5 }?.key

        val totalScore = sorted.sumOf { it.value }.coerceAtLeast(0.001)
        val primaryWeight = sorted[0].value / totalScore
        val confidence = computeConfidence(primaryWeight, lab, lightness, chroma, temperature, thresholds)

        return SeasonResult(
            primarySeason  = primary,
            secondarySeason = secondary,
            primaryWeight  = primaryWeight,
            temperature    = temperature,
            confidence     = confidence,
            lightness      = lightness,
            chroma         = chroma,
            explanation    = buildExplanation(primary, secondary, temperature, lightness, chroma, confidence)
        )
    }

    private fun classifyLightness(l: Double, t: SeasonThresholds) = when {
        l > t.lightL -> LightnessCategory.LIGHT
        l < t.deepL  -> LightnessCategory.DEEP
        else         -> LightnessCategory.MEDIUM
    }

    private fun classifyChroma(c: Double, t: SeasonThresholds) = when {
        c > t.brightC                            -> ChromaCategory.BRIGHT
        c < t.mutedC                             -> ChromaCategory.NEUTRAL
        else                                     -> ChromaCategory.MUTED
    }

    private fun inferTemperature(
        lab: LabColor,
        hint: ColorTemperature?,
        t: SeasonThresholds
    ): ColorTemperature {
        if (lab.chroma < t.neutralChromaThreshold) {
            return hint ?: ColorTemperature.NEUTRAL
        }
        val h = lab.hueAngle
        val warmScore = hueArcScore(h, t.warmHueMin, t.warmHueMax) +
                        if (lab.a > 5 && lab.b > 5) 0.3 else 0.0  // a+, b+ tint
        val coolScore = hueArcScore(h, t.coolHueMin, t.coolHueMax) +
                        if (lab.a < -3 || (lab.b < 0)) 0.3 else 0.0

        return when {
            warmScore > 0.6 -> ColorTemperature.WARM
            coolScore > 0.6 -> ColorTemperature.COOL
            hint != null    -> hint
            else            -> if (warmScore > coolScore) ColorTemperature.WARM else ColorTemperature.COOL
        }
    }

    /** Returns 0–1 score of how centrally [h] falls in the hue arc [min]..[max]. */
    private fun hueArcScore(h: Double, min: Double, max: Double): Double {
        val mid = (min + max) / 2.0
        val half = (max - min) / 2.0
        val diff = abs(h - mid).let { if (it > 180) 360 - it else it }
        return (1.0 - (diff / half)).coerceIn(0.0, 1.0)
    }

    private fun computeSeasonScores(
        lab: LabColor,
        lightness: LightnessCategory,
        chroma: ChromaCategory,
        temperature: ColorTemperature,
        t: SeasonThresholds
    ): Map<Season, Double> {
        // Membership degrees for each axis
        val warmDeg = when (temperature) {
            ColorTemperature.WARM    -> 1.0
            ColorTemperature.NEUTRAL -> 0.5
            ColorTemperature.COOL    -> 0.0
        }
        val coolDeg = 1.0 - warmDeg

        val lightDeg = gaussianMembership(lab.l, t.lightL + 5, 15.0)
        val mediumDeg = gaussianMembership(lab.l, (t.lightL + t.deepL) / 2, 15.0)
        val deepDeg = gaussianMembership(lab.l, t.deepL - 5, 15.0)

        val brightDeg = gaussianMembership(lab.chroma, t.brightC + 10, 12.0)
        val mutedDeg = gaussianMembership(lab.chroma, (t.brightC + t.mutedC) / 2, 12.0)

        return mapOf(
            // Spring: Warm + Light/Medium + Bright/Clear
            Season.SPRING to warmDeg * (lightDeg + mediumDeg) * brightDeg,
            // Summer: Cool + Light/Medium + Muted/Soft
            Season.SUMMER to coolDeg * (lightDeg + mediumDeg) * mutedDeg,
            // Autumn: Warm + Medium/Deep + Muted/Earthy
            Season.AUTUMN to warmDeg * (mediumDeg + deepDeg) * mutedDeg,
            // Winter: Cool + Deep + Bright/High-contrast
            Season.WINTER to coolDeg * deepDeg * brightDeg
        )
    }

    /** Gaussian membership function — soft boundary instead of hard threshold. */
    private fun gaussianMembership(x: Double, mean: Double, sigma: Double): Double {
        val diff = x - mean
        return exp(-(diff * diff) / (2 * sigma * sigma))
    }

    private fun computeConfidence(
        primaryWeight: Double,
        lab: LabColor,
        lightness: LightnessCategory,
        chroma: ChromaCategory,
        temperature: ColorTemperature,
        t: SeasonThresholds
    ): Double {
        var conf = primaryWeight   // 0–1 from score dominance

        // Penalise near-neutral colours (low chroma → ambiguous season)
        if (lab.chroma < t.mutedC) conf *= 0.7
        if (lab.chroma < t.neutralChromaThreshold) conf *= 0.6

        // Penalise neutral temperature (ambiguous warm/cool)
        if (temperature == ColorTemperature.NEUTRAL) conf *= 0.8

        return conf.coerceIn(0.0, 1.0)
    }

    private fun buildExplanation(
        primary: Season,
        secondary: Season?,
        temperature: ColorTemperature,
        lightness: LightnessCategory,
        chroma: ChromaCategory,
        confidence: Double
    ): String {
        val tempLabel  = temperature.name.lowercase()
        val lightLabel = lightness.name.lowercase()
        val chromaLabel = when (chroma) {
            ChromaCategory.BRIGHT  -> "clear and vivid"
            ChromaCategory.MUTED   -> "soft and muted"
            ChromaCategory.NEUTRAL -> "neutral"
        }
        val confLabel = when {
            confidence > 0.75 -> "Strong"
            confidence > 0.50 -> "Moderate"
            else              -> "Low"
        }

        val base = "This colour is $tempLabel, $lightLabel in value, and $chromaLabel in saturation, " +
                   "which places it in the ${primary.displayName} palette."

        val secondaryNote = secondary?.let {
            " It sits near the boundary of ${it.displayName}, so ${it.displayName} tones also work well."
        } ?: ""

        return "$confLabel confidence ${primary.displayName}. $base$secondaryNote"
    }

    /** Extended 12-season classification stub — refines the primary 4-season result. */
    fun classifyExtended(
        lab: LabColor,
        base: SeasonResult
    ): String {
        return when (base.primarySeason) {
            Season.SPRING -> when {
                lab.l > 68 && lab.chroma > 45 -> "Light Spring"
                lab.hueAngle in 20.0..50.0    -> "Warm Spring (True)"
                else                           -> "Clear Spring"
            }
            Season.SUMMER -> when {
                lab.l > 65                     -> "Light Summer"
                lab.chroma < 18                -> "Soft Summer"
                else                           -> "Cool Summer (True)"
            }
            Season.AUTUMN -> when {
                lab.l < 38                     -> "Deep Autumn"
                lab.hueAngle in 30.0..60.0    -> "Warm Autumn (True)"
                else                           -> "Soft Autumn"
            }
            Season.WINTER -> when {
                lab.l < 30                     -> "Deep Winter"
                lab.chroma < 25                -> "Cool Winter"
                else                           -> "Clear Winter (True)"
            }
        }
    }
}
