package com.hue.core.color.algorithm

import com.hue.core.color.model.RgbColor
import com.hue.core.color.model.XyzColor

/**
 * Bradford chromatic adaptation transform (CAT).
 *
 * Used to adapt colours measured under a non-D65 illuminant (e.g. tungsten A,
 * fluorescent F2) to the D65 reference white assumed by the PANTONE FHI LAB values.
 *
 * Reference: Lindbloom, "Chromatic Adaptation", brucelindbloom.com.
 */
object BradfordAdaptation {

    // Bradford M_A matrix  (M_A * XYZ → cone-response space)
    private val MA = arrayOf(
        doubleArrayOf( 0.8951,  0.2664, -0.1614),
        doubleArrayOf(-0.7502,  1.7135,  0.0367),
        doubleArrayOf( 0.0389, -0.0685,  1.0296)
    )

    // Bradford M_A inverse
    private val MA_INV = arrayOf(
        doubleArrayOf( 0.9869929, -0.1470543,  0.1599627),
        doubleArrayOf( 0.4323053,  0.5183603,  0.0492912),
        doubleArrayOf(-0.0085287,  0.0400428,  0.9684867)
    )

    // D65 white point in XYZ
    private val D65 = XyzColor(95.047, 100.000, 108.883)

    // CIE standard illuminants (X, Y, Z) — from ISO 11664-2
    val illuminants = mapOf(
        "D65" to XyzColor(95.047,  100.000, 108.883),
        "D50" to XyzColor(96.422,  100.000,  82.521),
        "A"   to XyzColor(109.850, 100.000,  35.585),   // Tungsten
        "F2"  to XyzColor(99.187,  100.000,  67.395),   // Cool fluorescent
        "F7"  to XyzColor(95.044,  100.000, 108.755),   // D65 simulator
        "F11" to XyzColor(100.966, 100.000,  64.370)    // Tri-phosphor warm
    )

    private fun matVec(m: Array<DoubleArray>, v: DoubleArray): DoubleArray =
        DoubleArray(3) { i -> m[i][0] * v[0] + m[i][1] * v[1] + m[i][2] * v[2] }

    /**
     * Adapt an XYZ colour from [srcIlluminant] to D65.
     * If [srcIlluminant] is already D65 this is a no-op (returns input unchanged).
     */
    fun adaptToD65(xyz: XyzColor, srcIlluminant: XyzColor): XyzColor {
        if (srcIlluminant == D65) return xyz

        val ws = matVec(MA, doubleArrayOf(srcIlluminant.x, srcIlluminant.y, srcIlluminant.z))
        val wd = matVec(MA, doubleArrayOf(D65.x, D65.y, D65.z))

        val rho = wd[0] / ws[0]; val gam = wd[1] / ws[1]; val bet = wd[2] / ws[2]

        val src = matVec(MA, doubleArrayOf(xyz.x, xyz.y, xyz.z))
        val adapted = doubleArrayOf(src[0] * rho, src[1] * gam, src[2] * bet)
        val result = matVec(MA_INV, adapted)
        return XyzColor(result[0], result[1], result[2])
    }

    /**
     * Apply a white-balance correction to an RGB pixel value.
     * [srcIlluminant] is the estimated scene illuminant (from EXIF or heuristic).
     */
    fun correctRgb(rgb: RgbColor, srcIlluminant: XyzColor): RgbColor {
        val xyz = ColorConverter.rgbToXyz(rgb)
        val adapted = adaptToD65(xyz, srcIlluminant)
        return ColorConverter.xyzToRgb(adapted)
    }

    /**
     * Estimate the scene illuminant from the average mid-tone cast of an image.
     * Returns the closest standard illuminant.
     *
     * @param avgR  average red   of mid-tone pixels (L* 30–70)
     * @param avgG  average green of mid-tone pixels
     * @param avgB  average blue  of mid-tone pixels
     */
    fun estimateIlluminant(avgR: Double, avgG: Double, avgB: Double): XyzColor {
        // Simple grey-world assumption: neutral scene → equal RGB per channel
        val total = avgR + avgG + avgB
        if (total < 1.0) return D65
        val rRatio = avgR / total * 3
        val bRatio = avgB / total * 3
        return when {
            rRatio > 1.12 && bRatio < 0.88 -> illuminants["A"]!!   // warm tungsten
            bRatio > 1.08 && rRatio < 0.92 -> illuminants["F2"]!!  // cool fluorescent
            else                            -> D65
        }
    }
}
