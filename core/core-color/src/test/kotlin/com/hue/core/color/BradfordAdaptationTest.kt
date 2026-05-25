package com.hue.core.color

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.BradfordAdaptation
import com.hue.core.color.algorithm.ColorConverter
import com.hue.core.color.model.RgbColor
import com.hue.core.color.model.XyzColor
import org.junit.Test
import kotlin.math.abs

class BradfordAdaptationTest {

    @Test
    fun `D65 illuminant is identity transform`() {
        val rgb = RgbColor(180, 92, 45)
        val d65 = BradfordAdaptation.illuminants["D65"]!!
        val corrected = BradfordAdaptation.correctRgb(rgb, d65)
        assertThat(abs(corrected.r - rgb.r)).isAtMost(2)
        assertThat(abs(corrected.g - rgb.g)).isAtMost(2)
        assertThat(abs(corrected.b - rgb.b)).isAtMost(2)
    }

    @Test
    fun `tungsten illuminant shifts image cooler`() {
        // A warm tungsten-lit orange should shift towards cooler after D65 adaptation
        val warmRgb = RgbColor(255, 200, 100)
        val tungstenIlluminant = BradfordAdaptation.illuminants["A"]!!
        val corrected = BradfordAdaptation.correctRgb(warmRgb, tungstenIlluminant)
        // After correction the red and green channels should decrease relative to blue
        val warmRatio = warmRgb.r.toDouble() / warmRgb.b.toDouble()
        val coolRatio = corrected.r.toDouble() / corrected.b.toDouble()
        assertThat(coolRatio).isLessThan(warmRatio)
    }

    @Test
    fun `adaptToD65 with D65 source is identity`() {
        val xyz = XyzColor(50.0, 40.0, 30.0)
        val d65 = BradfordAdaptation.illuminants["D65"]!!
        val adapted = BradfordAdaptation.adaptToD65(xyz, d65)
        assertThat(abs(adapted.x - xyz.x)).isLessThan(0.01)
        assertThat(abs(adapted.y - xyz.y)).isLessThan(0.01)
        assertThat(abs(adapted.z - xyz.z)).isLessThan(0.01)
    }

    @Test
    fun `estimateIlluminant detects warm cast`() {
        // Warm scene: high R, low B
        val illuminant = BradfordAdaptation.estimateIlluminant(200.0, 150.0, 100.0)
        assertThat(illuminant.x).isGreaterThan(100.0)  // A illuminant has high X
    }

    @Test
    fun `estimateIlluminant detects cool cast`() {
        // Cool scene: low R, high B
        val illuminant = BradfordAdaptation.estimateIlluminant(100.0, 150.0, 210.0)
        assertThat(illuminant.z).isGreaterThan(illuminant.x)  // F2 or cool: higher Z
    }

    @Test
    fun `estimateIlluminant returns D65 for neutral scene`() {
        val d65 = BradfordAdaptation.illuminants["D65"]!!
        val illuminant = BradfordAdaptation.estimateIlluminant(150.0, 150.0, 150.0)
        assertThat(abs(illuminant.x - d65.x)).isLessThan(5.0)
        assertThat(abs(illuminant.y - d65.y)).isLessThan(5.0)
    }
}
