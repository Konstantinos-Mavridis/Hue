package com.hue.core.color.algorithm

import android.graphics.Bitmap
import com.hue.core.color.model.DominantColorResult
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.RgbColor
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * K-means dominant colour extractor operating in CIELAB space.
 *
 * Why LAB?  Euclidean distance in LAB is more perceptually uniform than in RGB,
 * so clusters formed here correspond more closely to what the human eye perceives
 * as distinct colours.  ΔE (basic) ~ Euclidean distance in LAB.
 *
 * Algorithm:
 *  1. Downsample bitmap to ≤ TARGET_SIDE × TARGET_SIDE pixels.
 *  2. Convert every pixel to LAB.
 *  3. Run k-means (k = K, max MAX_ITER iterations, early-exit on centroid stability).
 *  4. Reject achromatic, near-black, and near-white clusters.
 *  5. Return the highest-weight chromatic cluster.
 */
object KMeansDominantColor {

    private const val TARGET_SIDE = 60
    private const val K            = 5
    private const val MAX_ITER     = 25
    private const val STABILITY_THRESHOLD = 0.5  // centroid movement (LAB units) to declare convergence

    // Thresholds to exclude achromatic / extreme clusters
    private const val MIN_CHROMA   = 8.0   // C* below this → achromatic
    private const val MIN_L        = 12.0  // below → near-black
    private const val MAX_L        = 92.0  // above → near-white / blown out

    data class Cluster(
        val centroid: LabColor,
        val pixelCount: Int,
        val totalPixels: Int
    ) {
        val fraction: Double get() = pixelCount.toDouble() / totalPixels
    }

    /**
     * Extract the dominant colour from [bitmap] after applying optional
     * white-balance correction via [illuminantCorrectedPixels] (pre-processed RGB list).
     *
     * If [illuminantCorrectedPixels] is null, pixels are read directly from the bitmap.
     */
    fun extract(
        bitmap: Bitmap,
        illuminantCorrectedPixels: List<RgbColor>? = null
    ): DominantColorResult {
        val scaled = scaleBitmap(bitmap)
        val pixels = illuminantCorrectedPixels ?: readPixels(scaled)
        val labPixels = pixels.map { ColorConverter.rgbToLab(it) }

        val variance = computeVariance(labPixels)
        val clusters = kMeans(labPixels)

        val chromatic = clusters
            .filter { it.centroid.chroma >= MIN_CHROMA }
            .filter { it.centroid.l in MIN_L..MAX_L }
            .sortedByDescending { it.fraction }

        val best = chromatic.firstOrNull() ?: clusters.maxByOrNull { it.fraction }!!

        val dominantRgb = ColorConverter.labToRgb(best.centroid)
        return DominantColorResult(
            rgb = dominantRgb,
            lab = best.centroid,
            clusterPixelFraction = best.fraction,
            regionVariance = variance
        )
    }

    private fun scaleBitmap(src: Bitmap): Bitmap {
        val scale = TARGET_SIDE.toFloat() / maxOf(src.width, src.height)
        if (scale >= 1f) return src
        val w = (src.width * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private fun readPixels(bitmap: Bitmap): List<RgbColor> {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.map { RgbColor.fromColorInt(it) }
    }

    private fun computeVariance(labs: List<LabColor>): Double {
        if (labs.isEmpty()) return 0.0
        val meanL = labs.sumOf { it.l } / labs.size
        val meanA = labs.sumOf { it.a } / labs.size
        val meanB = labs.sumOf { it.b } / labs.size
        return labs.sumOf { lab ->
            (lab.l - meanL).let { it * it } +
            (lab.a - meanA).let { it * it } +
            (lab.b - meanB).let { it * it }
        } / labs.size
    }

    private fun kMeans(pixels: List<LabColor>): List<Cluster> {
        if (pixels.isEmpty()) return emptyList()
        val k = minOf(K, pixels.size)

        // K-means++ initialisation for better convergence
        val centroids = kMeansPlusPlus(pixels, k).toMutableList()
        var assignments = IntArray(pixels.size) { 0 }

        repeat(MAX_ITER) { iter ->
            // Assign each pixel to nearest centroid
            val newAssignments = IntArray(pixels.size) { i ->
                centroids.indices.minByOrNull { c -> labDistSq(pixels[i], centroids[c]) }!!
            }

            // Early exit on stability
            val changed = newAssignments.indices.count { newAssignments[it] != assignments[it] }
            assignments = newAssignments
            if (iter > 0 && changed == 0) return@repeat

            // Recompute centroids
            centroids.indices.forEach { c ->
                val members = pixels.indices.filter { assignments[it] == c }.map { pixels[it] }
                if (members.isNotEmpty()) {
                    centroids[c] = LabColor(
                        members.sumOf { it.l } / members.size,
                        members.sumOf { it.a } / members.size,
                        members.sumOf { it.b } / members.size
                    )
                }
            }
        }

        val counts = IntArray(k)
        assignments.forEach { counts[it]++ }
        return centroids.mapIndexed { i, c ->
            Cluster(centroid = c, pixelCount = counts[i], totalPixels = pixels.size)
        }
    }

    private fun kMeansPlusPlus(pixels: List<LabColor>, k: Int): List<LabColor> {
        val chosen = mutableListOf(pixels.random())
        repeat(k - 1) {
            val distances = pixels.map { p ->
                chosen.minOf { c -> labDistSq(p, c) }
            }
            val sum = distances.sum()
            var rand = Random.nextDouble() * sum
            var idx = 0
            for (i in distances.indices) {
                rand -= distances[i]
                if (rand <= 0) { idx = i; break }
            }
            chosen.add(pixels[idx])
        }
        return chosen
    }

    private fun labDistSq(a: LabColor, b: LabColor): Double {
        val dl = a.l - b.l; val da = a.a - b.a; val db = a.b - b.b
        return dl * dl + da * da + db * db
    }

    /** Assess if a region is too noisy for reliable colour extraction. */
    fun isRegionTooNoisy(variance: Double): Boolean = variance > 350.0

    /** Assess if a region has excessive highlights (overexposure). */
    fun hasGlare(labPixels: List<LabColor>): Boolean {
        val overexposed = labPixels.count { it.l > 90.0 }
        return overexposed.toDouble() / labPixels.size > 0.20
    }
}
