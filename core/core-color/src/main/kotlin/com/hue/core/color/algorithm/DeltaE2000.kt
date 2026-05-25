package com.hue.core.color.algorithm

import com.hue.core.color.model.LabColor
import kotlin.math.*

/**
 * CIE ΔE2000 colour-difference formula.
 *
 * Reference: Sharma et al., "The CIEDE2000 Color-Difference Formula:
 * Implementation Notes, Supplementary Test Data, and Mathematical Observations",
 * Color Research & Application, Vol 30, No. 1, 2005.
 *
 * This is the industry-standard formula for perceptual colour difference in
 * textiles (ISO 105-J03) and is what Pantone uses internally.
 */
object DeltaE2000 {

    /**
     * Computes ΔE2000 between two CIELAB colours.
     * Returns a positive scalar where:
     *   < 1.0  → imperceptible difference
     *   1–2    → just noticeable in controlled conditions
     *   2–5    → noticeable; acceptable for textile match
     *   ≥ 5    → clearly different colours
     */
    fun compute(lab1: LabColor, lab2: LabColor): Double {
        val lBar  = (lab1.l + lab2.l) / 2.0
        val c1    = sqrt(lab1.a.pow(2) + lab1.b.pow(2))
        val c2    = sqrt(lab2.a.pow(2) + lab2.b.pow(2))
        val cBar  = (c1 + c2) / 2.0
        val cBar7 = cBar.pow(7)
        val g     = 0.5 * (1 - sqrt(cBar7 / (cBar7 + 25.0.pow(7))))
        val a1p   = lab1.a * (1 + g)
        val a2p   = lab2.a * (1 + g)
        val c1p   = sqrt(a1p.pow(2) + lab1.b.pow(2))
        val c2p   = sqrt(a2p.pow(2) + lab2.b.pow(2))
        val cBarP = (c1p + c2p) / 2.0

        fun hprime(a: Double, b: Double): Double {
            if (abs(a) < 1e-10 && abs(b) < 1e-10) return 0.0
            val h = Math.toDegrees(atan2(b, a))
            return if (h < 0) h + 360 else h
        }

        val h1p = hprime(a1p, lab1.b)
        val h2p = hprime(a2p, lab2.b)

        val dhp = when {
            abs(c1p) < 1e-10 || abs(c2p) < 1e-10 -> 0.0
            abs(h2p - h1p) <= 180                  -> h2p - h1p
            h2p - h1p > 180                         -> h2p - h1p - 360
            else                                    -> h2p - h1p + 360
        }

        val dLp = lab2.l - lab1.l
        val dCp = c2p - c1p
        val dHp = 2 * sqrt(c1p * c2p) * sin(Math.toRadians(dhp / 2))

        val hBarP = when {
            abs(c1p) < 1e-10 || abs(c2p) < 1e-10 -> h1p + h2p
            abs(h1p - h2p) <= 180                  -> (h1p + h2p) / 2
            h1p + h2p < 360                         -> (h1p + h2p + 360) / 2
            else                                    -> (h1p + h2p - 360) / 2
        }

        val t = 1 -
                0.17 * cos(Math.toRadians(hBarP - 30)) +
                0.24 * cos(Math.toRadians(2 * hBarP)) +
                0.32 * cos(Math.toRadians(3 * hBarP + 6)) -
                0.20 * cos(Math.toRadians(4 * hBarP - 63))

        val lBar50 = (lBar - 50).pow(2)
        val sl = 1 + 0.015 * lBar50 / sqrt(20 + lBar50)
        val sc = 1 + 0.045 * cBarP
        val sh = 1 + 0.015 * cBarP * t

        val cBarP7 = cBarP.pow(7)
        val rc = 2 * sqrt(cBarP7 / (cBarP7 + 25.0.pow(7)))
        val dTheta = 30 * exp(-((hBarP - 275) / 25).pow(2))
        val rt = -sin(Math.toRadians(2 * dTheta)) * rc

        return sqrt(
            (dLp / sl).pow(2) +
            (dCp / sc).pow(2) +
            (dHp / sh).pow(2) +
            rt * (dCp / sc) * (dHp / sh)
        )
    }
}
