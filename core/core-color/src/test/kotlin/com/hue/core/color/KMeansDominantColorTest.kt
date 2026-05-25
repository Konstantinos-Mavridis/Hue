package com.hue.core.color

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.KMeansDominantColor
import com.hue.core.color.model.LabColor
import com.hue.core.color.model.RgbColor
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KMeansDominantColorTest {

    // ── isRegionTooNoisy ──────────────────────────────────────────────────────

    @Test fun `low variance region is not too noisy`() {
        assertThat(KMeansDominantColor.isRegionTooNoisy(50.0)).isFalse()
    }

    @Test fun `high variance region is too noisy`() {
        assertThat(KMeansDominantColor.isRegionTooNoisy(400.0)).isTrue()
    }

    @Test fun `boundary variance just above 350 is too noisy`() {
        assertThat(KMeansDominantColor.isRegionTooNoisy(350.1)).isTrue()
    }

    @Test fun `zero variance is not noisy`() {
        assertThat(KMeansDominantColor.isRegionTooNoisy(0.0)).isFalse()
    }

    // ── hasGlare ──────────────────────────────────────────────────────────────

    @Test fun `region with no overexposed pixels has no glare`() {
        val pixels = List(100) { LabColor(55.0, 10.0, 10.0) }
        assertThat(KMeansDominantColor.hasGlare(pixels)).isFalse()
    }

    @Test fun `region with more than 20 percent L above 90 has glare`() {
        val overexposed = List(25) { LabColor(92.0, 0.0, 0.0) }
        val normal      = List(75) { LabColor(50.0, 10.0, 10.0) }
        assertThat(KMeansDominantColor.hasGlare(overexposed + normal)).isTrue()
    }

    @Test fun `region with exactly 20 percent L above 90 has no glare`() {
        val overexposed = List(20) { LabColor(91.0, 0.0, 0.0) }
        val normal      = List(80) { LabColor(50.0, 10.0, 10.0) }
        // 20/100 = 0.20, threshold is > 0.20, so no glare
        assertThat(KMeansDominantColor.hasGlare(overexposed + normal)).isFalse()
    }

    @Test fun `empty pixel list has no glare`() {
        assertThat(KMeansDominantColor.hasGlare(emptyList())).isFalse()
    }

    // ── extract with pre-supplied pixels ─────────────────────────────────────

    private fun mockSmallBitmap(w: Int = 4, h: Int = 4): Bitmap {
        val bmp = mockk<Bitmap>()
        every { bmp.width }  returns w
        every { bmp.height } returns h
        return bmp
    }

    @Test fun `extract returns dominant chromatic colour from uniform orange region`() {
        val bitmap = mockSmallBitmap()
        val pixels = List(16) { RgbColor(200, 80, 30) }
        val result = KMeansDominantColor.extract(bitmap, pixels)
        // Dominant cluster should be close to the input orange
        assertThat(result.rgb.r).isGreaterThan(result.rgb.b)
        assertThat(result.rgb.r).isAtLeast(150)
    }

    @Test fun `extract with mixed colours picks majority chromatic cluster`() {
        val bitmap = mockSmallBitmap()
        val pixels = List(12) { RgbColor(210, 80, 30) } +   // orange × 12
                     List(4)  { RgbColor(30, 50, 200) }      // blue × 4
        val result = KMeansDominantColor.extract(bitmap, pixels)
        // Result should be orange-dominant (higher R than B)
        assertThat(result.rgb.r).isGreaterThan(result.rgb.b)
    }

    @Test fun `extract cluster fraction is between 0 and 1`() {
        val bitmap = mockSmallBitmap()
        val pixels = List(16) { RgbColor(180, 90, 50) }
        val result = KMeansDominantColor.extract(bitmap, pixels)
        assertThat(result.clusterPixelFraction).isAtLeast(0.0)
        assertThat(result.clusterPixelFraction).isAtMost(1.0)
    }

    @Test fun `extract region variance is positive for varied pixels`() {
        val bitmap = mockSmallBitmap()
        val pixels = List(8) { RgbColor(200, 80, 30) } +
                     List(8) { RgbColor(30, 150, 200) }
        val result = KMeansDominantColor.extract(bitmap, pixels)
        assertThat(result.regionVariance).isGreaterThan(0.0)
    }

    @Test fun `extract region variance is near zero for uniform pixels`() {
        val bitmap = mockSmallBitmap()
        val pixels = List(20) { RgbColor(180, 90, 45) }
        val result = KMeansDominantColor.extract(bitmap, pixels)
        assertThat(result.regionVariance).isLessThan(10.0)
    }

    @Test fun `extract LAB L star is in plausible range for medium orange`() {
        val bitmap = mockSmallBitmap()
        val pixels = List(16) { RgbColor(180, 90, 45) }
        val result = KMeansDominantColor.extract(bitmap, pixels)
        assertThat(result.lab.l).isGreaterThan(30.0)
        assertThat(result.lab.l).isLessThan(80.0)
    }

    @Test fun `extract returns result even for single pixel input`() {
        val bitmap = mockSmallBitmap(1, 1)
        val pixels = listOf(RgbColor(150, 100, 60))
        val result = KMeansDominantColor.extract(bitmap, pixels)
        assertThat(result).isNotNull()
    }

    @Test fun `isRegionTooNoisy threshold is 350`() {
        assertThat(KMeansDominantColor.isRegionTooNoisy(349.9)).isFalse()
        assertThat(KMeansDominantColor.isRegionTooNoisy(350.0)).isFalse()
        assertThat(KMeansDominantColor.isRegionTooNoisy(350.001)).isTrue()
    }
}
