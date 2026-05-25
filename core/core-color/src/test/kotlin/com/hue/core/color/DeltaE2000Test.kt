package com.hue.core.color

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.algorithm.DeltaE2000
import com.hue.core.color.model.LabColor
import org.junit.Test
import kotlin.math.abs

/**
 * Reference test pairs from Sharma et al. 2005, Table 1.
 * Published ΔE2000 values are used as ground truth.
 */
class DeltaE2000Test {

    private data class TestPair(
        val l1: Double, val a1: Double, val b1: Double,
        val l2: Double, val a2: Double, val b2: Double,
        val expected: Double
    )

    // Sharma et al. 2005, Table 1 — 34 test pairs (selected subset)
    private val sharmaTestPairs = listOf(
        TestPair(50.0000,  2.6772, -79.7751, 50.0000,  0.0000, -82.7485, 2.0425),
        TestPair(50.0000,  3.1571, -77.2803, 50.0000,  0.0000, -82.7485, 2.8615),
        TestPair(50.0000,  2.8361, -74.0200, 50.0000,  0.0000, -82.7485, 3.4412),
        TestPair(50.0000, -1.3802, -84.2814, 50.0000,  0.0000, -82.7485, 1.0000),
        TestPair(50.0000, -1.1848, -84.8006, 50.0000,  0.0000, -82.7485, 1.0000),
        TestPair(50.0000, -0.9009, -85.5211, 50.0000,  0.0000, -82.7485, 1.0000),
        TestPair(50.0000,  0.0000,   0.0000, 50.0000, -1.0000,   2.0000, 2.3669),
        TestPair(50.0000, -1.0000,   2.0000, 50.0000,  0.0000,   0.0000, 2.3669),
        TestPair(50.0000,  2.4900,  -0.0010, 50.0000, -2.4900,   0.0009, 7.1792),
        TestPair(50.0000,  2.4900,  -0.0010, 50.0000, -2.4900,   0.0010, 7.1792),
        TestPair(50.0000,  2.4900,  -0.0010, 50.0000, -2.4900,   0.0011, 7.2195),
        TestPair(50.0000,  2.4900,  -0.0010, 50.0000, -2.4900,   0.0012, 7.2195),
        TestPair(50.0000, -0.0010,   2.4900, 50.0000,  0.0009,  -2.4900, 4.8045),
        TestPair(50.0000, -0.0010,   2.4900, 50.0000,  0.0010,  -2.4900, 4.8045),
        TestPair(50.0000, -0.0010,   2.4900, 50.0000,  0.0011,  -2.4900, 4.7461),
        TestPair(50.0000,  2.5000,   0.0000, 50.0000,  0.0000,  -2.5000, 4.3065),
        TestPair(50.0000,  2.5000,   0.0000, 73.0000,  25.0000, -18.0000, 27.1492),
        TestPair(50.0000,  2.5000,   0.0000, 61.0000,  -5.0000,  29.0000, 22.8977),
        TestPair(50.0000,  2.5000,   0.0000, 56.0000, -27.0000,  -3.0000, 31.9030),
        TestPair(50.0000,  2.5000,   0.0000, 58.0000,  24.0000,  15.0000, 19.4535),
        TestPair(50.0000,  2.5000,   0.0000, 50.0000,   3.1736,   0.5854,  1.0000),
        TestPair(50.0000,  2.5000,   0.0000, 50.0000,   3.2972,   0.0000,  1.0000),
        TestPair(50.0000,  2.5000,   0.0000, 50.0000,   1.8634,   0.5757,  1.0000),
        TestPair(50.0000,  2.5000,   0.0000, 50.0000,   3.2592,   0.3350,  1.0000),
        TestPair(60.2574, -34.0099,  36.2677, 60.4626, -34.1751, 39.4387,  1.2644),
        TestPair(63.0109, -31.0961,  -5.8663, 62.8187, -29.7946, -4.0864,  1.2630),
        TestPair(61.2901,  3.7196,  -5.3901, 61.4292,  2.2480,  -4.9620,  1.8731),
        TestPair(35.0831, -44.1164,   3.7933, 35.0232, -40.0716,  1.5901,  1.8645),
        TestPair(22.7233,  20.0904, -46.6940, 23.0331, 14.9730, -42.5619,  2.0373),
        TestPair(36.4612,  47.8580,  18.3852, 36.2715, 50.5065, 21.2231,   1.4146),
        TestPair(90.8027,  -2.0831,   1.4410, 91.1528,  -1.6435,  0.0447,  1.4441),
        TestPair(90.9257,  -0.5406,  -0.9208, 88.6381, -0.8985, -0.7239,   1.5381),
        TestPair( 6.7747,  -0.2908,  -2.4247,  5.8714, -0.0985, -2.2286,   0.6377),
        TestPair( 2.0776,   0.0795,  -1.1350,  0.9033, -0.0636, -0.5514,   0.9082)
    )

    @Test
    fun `all Sharma 2005 test pairs within tolerance 0_005`() {
        val failed = mutableListOf<String>()
        sharmaTestPairs.forEachIndexed { i, pair ->
            val actual = DeltaE2000.compute(
                LabColor(pair.l1, pair.a1, pair.b1),
                LabColor(pair.l2, pair.a2, pair.b2)
            )
            if (abs(actual - pair.expected) > 0.005) {
                failed.add("Pair ${i + 1}: expected=${pair.expected}, actual=${String.format("%.4f", actual)}")
            }
        }
        assertThat(failed).isEmpty()
    }

    @Test
    fun `identical colours have deltaE of zero`() {
        val lab = LabColor(50.0, 20.0, -30.0)
        assertThat(DeltaE2000.compute(lab, lab)).isLessThan(0.001)
    }

    @Test
    fun `symmetric deltaE(a,b) equals deltaE(b,a)`() {
        val a = LabColor(45.0, 30.0, -20.0)
        val b = LabColor(60.0, -10.0, 40.0)
        assertThat(abs(DeltaE2000.compute(a, b) - DeltaE2000.compute(b, a))).isLessThan(0.001)
    }

    @Test
    fun `black vs white has very high deltaE`() {
        val black = LabColor(0.0, 0.0, 0.0)
        val white = LabColor(100.0, 0.0, 0.0)
        assertThat(DeltaE2000.compute(black, white)).isGreaterThan(50.0)
    }

    @Test
    fun `near-identical colours have deltaE less than 1`() {
        val a = LabColor(50.01, 20.01, -30.01)
        val b = LabColor(50.00, 20.00, -30.00)
        assertThat(DeltaE2000.compute(a, b)).isLessThan(1.0)
    }
}
