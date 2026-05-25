package com.hue.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.hue.core.color.algorithm.BradfordAdaptation
import com.hue.core.color.algorithm.ColorConverter
import com.hue.core.color.algorithm.KMeansDominantColor
import com.hue.core.color.algorithm.SeasonClassifier
import com.hue.core.color.model.RgbColor
import com.hue.domain.model.*
import com.hue.domain.repository.PantoneRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class AnalyseFabricUseCase @Inject constructor(
    private val pantoneRepository: PantoneRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(input: AnalysisInput): Result<FabricAnalysis> =
        withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = loadBitmap(input.croppedBitmapPath)
                    ?: throw IllegalStateException("Cannot decode bitmap at ${input.croppedBitmapPath}")

                // 1. Estimate scene illuminant from EXIF or heuristic
                val illuminant = estimateIlluminant(input.croppedBitmapPath, bitmap)

                // 2. Apply illuminant correction to all pixels
                val correctedPixels = correctPixels(bitmap, illuminant)

                // 3. Extract dominant colour via k-means in LAB space
                val labPixels = correctedPixels.map { ColorConverter.rgbToLab(it) }

                if (KMeansDominantColor.hasGlare(labPixels)) {
                    // Still proceed but caller can surface the warning
                }

                val dominant = KMeansDominantColor.extract(bitmap, correctedPixels)

                // Guard: achromatic fabric
                if (dominant.lab.chroma < 8.0) {
                    val topMatches = pantoneRepository.findTopMatches(dominant.lab, 3)
                    val season = SeasonClassifier.classify(dominant.lab)
                    return@runCatching FabricAnalysis(
                        timestamp = System.currentTimeMillis(),
                        thumbnailPath = input.croppedBitmapPath,
                        dominantRgb = dominant.rgb,
                        dominantLab = dominant.lab,
                        topMatches = topMatches,
                        season = season,
                        regionVariance = dominant.regionVariance
                    )
                }

                // 4. PANTONE matching (top 3 by ΔE2000)
                val topMatches = pantoneRepository.findTopMatches(dominant.lab, 3)
                if (topMatches.isEmpty()) throw IllegalStateException("PANTONE database is empty")

                // 5. Season classification using best match's temperature hint
                val tempHint = topMatches.firstOrNull()?.color?.temperature
                val season = SeasonClassifier.classify(dominant.lab, tempHint)

                FabricAnalysis(
                    timestamp = System.currentTimeMillis(),
                    thumbnailPath = input.croppedBitmapPath,
                    dominantRgb = dominant.rgb,
                    dominantLab = dominant.lab,
                    topMatches = topMatches,
                    season = season,
                    regionVariance = dominant.regionVariance
                )
            }
        }

    private fun loadBitmap(path: String): Bitmap? =
        BitmapFactory.decodeFile(path)

    private fun estimateIlluminant(path: String, bitmap: Bitmap) =
        try {
            val exif = ExifInterface(path)
            val wb = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
            // EXIF white balance: 0 = auto, 1 = manual
            val colorTemp = exif.getAttribute("ColorTemperature")?.toIntOrNull()
            when {
                colorTemp != null && colorTemp < 3500 -> BradfordAdaptation.illuminants["A"]!!
                colorTemp != null && colorTemp > 6000 -> BradfordAdaptation.illuminants["F2"]!!
                else -> estimateFromMidtones(bitmap)
            }
        } catch (_: Exception) {
            estimateFromMidtones(bitmap)
        }

    private fun estimateFromMidtones(bitmap: Bitmap): com.hue.core.color.model.XyzColor {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val midtones = pixels.filter { p ->
            val r = android.graphics.Color.red(p)
            val g = android.graphics.Color.green(p)
            val b = android.graphics.Color.blue(p)
            val avg = (r + g + b) / 3
            avg in 80..175
        }
        if (midtones.isEmpty()) return BradfordAdaptation.illuminants["D65"]!!
        val avgR = midtones.sumOf { android.graphics.Color.red(it).toDouble() } / midtones.size
        val avgG = midtones.sumOf { android.graphics.Color.green(it).toDouble() } / midtones.size
        val avgB = midtones.sumOf { android.graphics.Color.blue(it).toDouble() } / midtones.size
        return BradfordAdaptation.estimateIlluminant(avgR, avgG, avgB)
    }

    private fun correctPixels(
        bitmap: Bitmap,
        illuminant: com.hue.core.color.model.XyzColor
    ): List<RgbColor> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.map { p ->
            val rgb = RgbColor.fromColorInt(p)
            BradfordAdaptation.correctRgb(rgb, illuminant)
        }
    }
}
