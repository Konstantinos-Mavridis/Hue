package com.hue.core.color

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.ColorConverter
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.RgbColor
import org.junit.Test
import kotlin.math.abs

/**
 * Reference values from Bruce Lindbloom's colour calculator (brucelindbloom.com)
 * and the CIE 1976 standard.
 */
class ColorConverterTest {

    private fun assertLabClose(actual: LabColor, expL: Double, expA: Double, expB: Double, tol: Double = 0.5) {
        assertThat(abs(actual.l - expL)).isLessThan(tol)
        assertThat(abs(actual.a - expA)).isLessThan(tol)
        assertThat(abs(actual.b - expB)).isLessThan(tol)
    }

    @Test
    fun `pure red converts to correct LAB`() {
        val lab = ColorConverter.rgbToLab(RgbColor(255, 0, 0))
        // Reference: L*=53.23, a*=80.11, b*=67.22 (D65, 2°)
        assertLabClose(lab, 53.23, 80.11, 67.22, tol = 1.0)
    }

    @Test
    fun `pure green converts to correct LAB`() {
        val lab = ColorConverter.rgbToLab(RgbColor(0, 255, 0))
        // Reference: L*=87.74, a*=-86.18, b*=83.18
        assertLabClose(lab, 87.74, -86.18, 83.18, tol = 1.0)
    }

    @Test
    fun `pure blue converts to correct LAB`() {
        val lab = ColorConverter.rgbToLab(RgbColor(0, 0, 255))
        // Reference: L*=32.30, a*=79.19, b*=-107.86
        assertLabClose(lab, 32.30, 79.19, -107.86, tol = 1.0)
    }

    @Test
    fun `black converts to L=0`() {
        val lab = ColorConverter.rgbToLab(RgbColor(0, 0, 0))
        assertThat(abs(lab.l)).isLessThan(0.1)
    }

    @Test
    fun `white converts to L near 100`() {
        val lab = ColorConverter.rgbToLab(RgbColor(255, 255, 255))
        assertThat(abs(lab.l - 100.0)).isLessThan(0.5)
        assertThat(abs(lab.a)).isLessThan(0.5)
        assertThat(abs(lab.b)).isLessThan(0.5)
    }

    @Test
    fun `mid grey is perceptually correct`() {
        val lab = ColorConverter.rgbToLab(RgbColor(128, 128, 128))
        // L* should be ~53.39 (not 50.2 as naive linear scaling would give)
        assertThat(lab.l).isGreaterThan(50.0)
        assertThat(lab.l).isLessThan(56.0)
        assertThat(abs(lab.a)).isLessThan(1.0)
        assertThat(abs(lab.b)).isLessThan(1.0)
    }

    @Test
    fun `round-trip RGB to LAB to RGB preserves values`() {
        val original = RgbColor(180, 92, 45)
        val lab = ColorConverter.rgbToLab(original)
        val recovered = ColorConverter.labToRgb(lab)
        assertThat(abs(recovered.r - original.r)).isAtMost(2)
        assertThat(abs(recovered.g - original.g)).isAtMost(2)
        assertThat(abs(recovered.b - original.b)).isAtMost(2)
    }

    @Test
    fun `chroma of achromatic colour is near zero`() {
        val lab = ColorConverter.rgbToLab(RgbColor(100, 100, 100))
        assertThat(lab.chroma).isLessThan(2.0)
    }

    @Test
    fun `chroma of vivid orange is high`() {
        val lab = ColorConverter.rgbToLab(RgbColor(255, 140, 0))
        assertThat(lab.chroma).isGreaterThan(60.0)
    }
}
