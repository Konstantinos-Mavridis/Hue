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
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(input: AnalysisInput): Result<FabricAnalysis> =
        withContext(Dispatchers.Default) {
            runCatching {
                val bitmap = loadBitmap(input.croppedBitmapPath)
                    ?: throw IllegalStateException("Cannot decode bitmap at ${input.croppedBitmapPath}")

                // Downsample to ≤200 px on the longest side before any pixel processing.
                // Camera crops can still be several megapixels; running Bradford adaptation
                // and K-means on millions of pixels causes 10+ minute runtimes on device.
                val analysisBitmap = downsample(bitmap, maxSide = 200)

                // 1. Estimate scene illuminant from EXIF or heuristic
                val illuminant = estimateIlluminant(input.croppedBitmapPath, analysisBitmap)

                // 2. Apply illuminant correction (now only ≤ 40 000 pixels)
                val correctedPixels = correctPixels(analysisBitmap, illuminant)

                // 3. Extract dominant colour via k-means in LAB space
                val labPixels = correctedPixels.map { ColorConverter.rgbToLab(it) }

                if (KMeansDominantColor.hasGlare(labPixels)) {
                    // Still proceed but caller can surface the warning
                }

                val dominant = KMeansDominantColor.extract(analysisBitmap, correctedPixels)

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

    private fun downsample(bitmap: Bitmap, maxSide: Int): Bitmap {
        val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (scale >= 1f) return bitmap
        val w = (bitmap.width  * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun loadBitmap(path: String): Bitmap? =
        BitmapFactory.decodeFile(path)

    private fun estimateIlluminant(path: String, bitmap: Bitmap) =
        try {
            val exif = ExifInterface(path)
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
