package com.hue.core.color.algorithm

import com.hue.core.color.model.HslColor
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.RgbColor
import com.hue.core.color.model.XyzColor
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Perceptually correct colour-space conversions.
 *
 * Pipeline:  RGB (sRGB) → linearRGB (inverse-gamma) → XYZ (D65) → CIELAB
 *
 * D65 reference white:  X_n = 95.047, Y_n = 100.0, Z_n = 108.883
 * Follows IEC 61966-2-1 for sRGB gamma and ISO 11664-4 for CIELAB.
 */
object ColorConverter {

    // D65 reference white (2° observer, CIE 1931)
    private const val Xn = 95.047
    private const val Yn = 100.000
    private const val Zn = 108.883

    // sRGB → linear light (inverse gamma)
    private fun srgbToLinear(c: Double): Double {
        val normalised = c / 255.0
        return if (normalised <= 0.04045) normalised / 12.92
        else ((normalised + 0.055) / 1.055).pow(2.4)
    }

    // Linear light → sRGB
    private fun linearToSrgb(c: Double): Double {
        val clamped = c.coerceIn(0.0, 1.0)
        return if (clamped <= 0.0031308) 255.0 * clamped * 12.92
        else 255.0 * (1.055 * clamped.pow(1.0 / 2.4) - 0.055)
    }

    // IEC 61966-2-1 sRGB linearisation matrix for D65
    fun rgbToXyz(rgb: RgbColor): XyzColor {
        val r = srgbToLinear(rgb.r.toDouble())
        val g = srgbToLinear(rgb.g.toDouble())
        val b = srgbToLinear(rgb.b.toDouble())
        return XyzColor(
            x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) * 100.0,
            y = (r * 0.2126729 + g * 0.7151522 + b * 0.0721750) * 100.0,
            z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) * 100.0
        )
    }

    fun xyzToRgb(xyz: XyzColor): RgbColor {
        val x = xyz.x / 100.0
        val y = xyz.y / 100.0
        val z = xyz.z / 100.0
        val r = linearToSrgb( x *  3.2404542 + y * -1.5371385 + z * -0.4985314)
        val g = linearToSrgb( x * -0.9692660 + y *  1.8760108 + z *  0.0415560)
        val b = linearToSrgb( x *  0.0556434 + y * -0.2040259 + z *  1.0572252)
        return RgbColor(r.roundToInt().coerceIn(0, 255),
                        g.roundToInt().coerceIn(0, 255),
                        b.roundToInt().coerceIn(0, 255))
    }

    private fun f(t: Double): Double {
        val delta = 6.0 / 29.0
        return if (t > delta.pow(3)) t.pow(1.0 / 3.0) else t / (3 * delta * delta) + 4.0 / 29.0
    }

    fun xyzToLab(xyz: XyzColor): LabColor {
        val fx = f(xyz.x / Xn)
        val fy = f(xyz.y / Yn)
        val fz = f(xyz.z / Zn)
        return LabColor(
            l = 116 * fy - 16,
            a = 500 * (fx - fy),
            b = 200 * (fy - fz)
        )
    }

    fun labToXyz(lab: LabColor): XyzColor {
        val fy = (lab.l + 16) / 116.0
        val fx = lab.a / 500.0 + fy
        val fz = fy - lab.b / 200.0
        val delta = 6.0 / 29.0
        fun finv(t: Double) = if (t > delta) t * t * t else (t - 4.0 / 29.0) * 3 * delta * delta
        return XyzColor(Xn * finv(fx), Yn * finv(fy), Zn * finv(fz))
    }

    fun rgbToLab(rgb: RgbColor): LabColor = xyzToLab(rgbToXyz(rgb))

    fun labToRgb(lab: LabColor): RgbColor = xyzToRgb(labToXyz(lab))

    fun rgbToHsl(rgb: RgbColor): HslColor {
        val r = rgb.r / 255.0
        val g = rgb.g / 255.0
        val b = rgb.b / 255.0
        val max = max(r, max(g, b))
        val min = min(r, min(g, b))
        val l = (max + min) / 2.0
        if (abs(max - min) < 1e-10) return HslColor(0.0, 0.0, l)
        val d = max - min
        val s = if (l > 0.5) d / (2.0 - max - min) else d / (max + min)
        val h = when (max) {
            r    -> ((g - b) / d + (if (g < b) 6 else 0)) / 6.0
            g    -> ((b - r) / d + 2) / 6.0
            else -> ((r - g) / d + 4) / 6.0
        }
        return HslColor(h * 360.0, s, l)
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}
