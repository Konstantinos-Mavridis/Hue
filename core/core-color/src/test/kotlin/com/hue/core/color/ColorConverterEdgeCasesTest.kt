package com.hue.core.color

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.ColorConverter
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.RgbColor
import org.junit.Test
import kotlin.math.abs

/** Edge-case and branch coverage for ColorConverter. */
class ColorConverterEdgeCasesTest {

    // ── LAB round-trip for all primary/secondary hues ─────────────────────

    @Test fun `cyan round-trips correctly`() {
        assertRoundTrip(RgbColor(0, 255, 255))
    }

    @Test fun `magenta round-trips correctly`() {
        assertRoundTrip(RgbColor(255, 0, 255))
    }

    @Test fun `yellow round-trips correctly`() {
        assertRoundTrip(RgbColor(255, 255, 0))
    }

    @Test fun `dark red round-trips correctly`() {
        assertRoundTrip(RgbColor(128, 0, 0))
    }

    @Test fun `navy round-trips correctly`() {
        assertRoundTrip(RgbColor(0, 0, 128))
    }

    @Test fun `olive round-trips correctly`() {
        assertRoundTrip(RgbColor(128, 128, 0))
    }

    @Test fun `teal round-trips correctly`() {
        assertRoundTrip(RgbColor(0, 128, 128))
    }

    @Test fun `purple round-trips correctly`() {
        assertRoundTrip(RgbColor(128, 0, 128))
    }

    private fun assertRoundTrip(rgb: RgbColor, tolerance: Int = 3) {
        val lab = ColorConverter.rgbToLab(rgb)
        val back = ColorConverter.labToRgb(lab)
        assertThat(abs(back.r - rgb.r)).isAtMost(tolerance)
        assertThat(abs(back.g - rgb.g)).isAtMost(tolerance)
        assertThat(abs(back.b - rgb.b)).isAtMost(tolerance)
    }

    // ── LAB L* monotonicity ───────────────────────────────────────────────

    @Test
    fun `increasing grey level produces increasing L star`() {
        val greys = listOf(0, 32, 64, 96, 128, 160, 192, 224, 255)
        val labs = greys.map { v -> ColorConverter.rgbToLab(RgbColor(v, v, v)).l }
        labs.zipWithNext().forEach { (a, b) ->
            assertThat(b).isGreaterThan(a)
        }
    }

    // ── Chroma of achromatic colours ──────────────────────────────────────

    @Test
    fun `neutral greys have chroma below 2`() {
        listOf(30, 60, 90, 120, 150, 180, 210).forEach { v ->
            val lab = ColorConverter.rgbToLab(RgbColor(v, v, v))
            assertThat(lab.chroma).isLessThan(2.0)
        }
    }

    // ── hueAngle coverage ─────────────────────────────────────────────────

    @Test
    fun `red LAB hue angle is in warm range`() {
        val lab = ColorConverter.rgbToLab(RgbColor(255, 0, 0))
        assertThat(lab.hueAngle).isAtLeast(0.0)
        assertThat(lab.hueAngle).isAtMost(360.0)
    }

    @Test
    fun `blue LAB hue angle wraps correctly to 0-360`() {
        val lab = ColorConverter.rgbToLab(RgbColor(0, 0, 255))
        assertThat(lab.hueAngle).isAtLeast(0.0)
        assertThat(lab.hueAngle).isAtMost(360.0)
    }

    @Test
    fun `hue angle for negative a positive b is in second quadrant`() {
        val lab = LabColor(50.0, -20.0, 30.0)  // b > 0, a < 0 → hue ~124°
        assertThat(lab.hueAngle).isAtLeast(90.0)
        assertThat(lab.hueAngle).isAtMost(180.0)
    }

    // ── HSL conversion ────────────────────────────────────────────────────

    @Test
    fun `achromatic colour has HSL saturation of 0`() {
        val hsl = ColorConverter.rgbToHsl(RgbColor(128, 128, 128))
        assertThat(hsl.s).isLessThan(0.001)
    }

    @Test
    fun `pure red has HSL hue near 0`() {
        val hsl = ColorConverter.rgbToHsl(RgbColor(255, 0, 0))
        assertThat(hsl.h).isLessThan(5.0)
        assertThat(hsl.s).isGreaterThan(0.99)
    }

    @Test
    fun `pure blue has HSL hue near 240`() {
        val hsl = ColorConverter.rgbToHsl(RgbColor(0, 0, 255))
        assertThat(abs(hsl.h - 240.0)).isLessThan(2.0)
    }

    @Test
    fun `pure green has HSL hue near 120`() {
        val hsl = ColorConverter.rgbToHsl(RgbColor(0, 255, 0))
        assertThat(abs(hsl.h - 120.0)).isLessThan(2.0)
    }

    @Test
    fun `black has HSL lightness 0`() {
        val hsl = ColorConverter.rgbToHsl(RgbColor(0, 0, 0))
        assertThat(hsl.l).isLessThan(0.001)
    }

    @Test
    fun `white has HSL lightness 1`() {
        val hsl = ColorConverter.rgbToHsl(RgbColor(255, 255, 255))
        assertThat(hsl.l).isGreaterThan(0.999)
    }

    // ── XYZ intermediate ──────────────────────────────────────────────────

    @Test
    fun `white XYZ is close to D65 reference white (normalised)`() {
        val xyz = ColorConverter.rgbToXyz(RgbColor(255, 255, 255))
        // Scaled by 100 in the impl — X≈95, Y≈100, Z≈109
        assertThat(abs(xyz.y - 100.0)).isLessThan(0.5)
    }

    @Test
    fun `black XYZ is near zero`() {
        val xyz = ColorConverter.rgbToXyz(RgbColor(0, 0, 0))
        assertThat(xyz.x).isLessThan(0.01)
        assertThat(xyz.y).isLessThan(0.01)
        assertThat(xyz.z).isLessThan(0.01)
    }

    // ── hex output ────────────────────────────────────────────────────────

    @Test
    fun `RgbColor hex includes hash prefix`() {
        assertThat(RgbColor(255, 0, 0).hex).startsWith("#")
    }

    @Test
    fun `RgbColor hex for pure red is correct`() {
        assertThat(RgbColor(255, 0, 0).hex).isEqualTo("#FF0000")
    }

    @Test
    fun `RgbColor hex pads single-digit values`() {
        assertThat(RgbColor(1, 2, 3).hex).isEqualTo("#010203")
    }

    @Test
    fun `RgbColor fromHex round-trips`() {
        val original = RgbColor(180, 92, 45)
        val hex = original.hex
        val back = RgbColor.fromHex(hex)
        assertThat(back).isEqualTo(original)
    }
}
