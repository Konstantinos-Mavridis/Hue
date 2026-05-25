# Hue — Fabric PANTONE Colour Expert

A production-grade native Android app that photographs or imports fabric swatches, extracts the dominant colour with illuminant correction, matches it to the closest PANTONE Fashion, Home + Interiors (FHI) colour(s), and classifies it into a colour season — giving stylists, wardrobe consultants, and textile professionals an accurate, explainable, offline-first colour analysis tool.

---

## Architecture

```
Hue/
├── app/                     # Entry point: Hilt, Navigation, MainActivity
├── core/
│   ├── core-color/          # All colour science (pure Kotlin, no Android deps)
│   └── core-design/         # Material 3 theme, shared UI components
├── data/
│   └── pantone-db/          # Room database, seed data, repository implementations
├── domain/                  # Use cases, domain models, repository interfaces
└── feature/
    ├── feature-capture/     # CameraX capture + crop/region-selection UI
    ├── feature-matching/    # Results screen (PANTONE chips, season card)
    ├── feature-season/      # Season classification components (reusable)
    └── feature-history/     # Saved scans list with filtering & search
```

**Stack:** Kotlin · Jetpack Compose · MVVM + Clean Architecture · Hilt · Room · CameraX · Coroutines + Flow · Material 3

---

## Colour Science Pipeline

### 1. Region Extraction
CameraX captures or the gallery picker imports a photo. The user selects a uniform region (minimum ~100 × 100 px effective). The crop is saved to the app cache.

### 2. Illuminant Estimation & Bradford Correction
EXIF metadata is read first (`TAG_WHITE_BALANCE`, `ColorTemperature`). If absent, a grey-world midtone average is computed and compared to known illuminant chromaticities. The Bradford chromatic adaptation transform (M_A matrix) normalises the pixel data to D65 before any colour measurement.

### 3. Dominant Colour Extraction (K-Means in LAB)
The corrected crop is downscaled to ≤ 60 × 60 px. K-Means++ (k = 5, max 25 iterations) runs on the CIELAB pixel values. Clusters with C* < 8 (achromatic), L* < 12 (near-black), or L* > 92 (blown-out) are rejected. The highest-pixel-count chromatic cluster is selected.

### 4. PANTONE FHI Matching (ΔE2000)
The dominant LAB colour is compared against every entry in the embedded PANTONE FHI database using the full CIE ΔE2000 formula (Sharma et al. 2005). The top 3 matches by ΔE value are returned.

**Match quality thresholds:**
| ΔE2000 | Quality |
|--------|---------|
| < 1.0 | Excellent (imperceptible) |
| 1–2 | Very Good |
| 2–5 | Good (acceptable for textiles) |
| 5–10 | Fair |
| ≥ 10 | Poor |

### 5. Colour Season Classification
Using LCH (cylindrical LAB) coordinates:

| Season | Temperature | Lightness (L*) | Chroma (C*) |
|--------|------------|-----------------|-------------|
| **Spring** | Warm | > 40 (light–medium) | > 35 (bright) |
| **Summer** | Cool | > 40 (light–medium) | 15–35 (muted) |
| **Autumn** | Warm | < 65 (medium–deep) | 15–35 (muted) |
| **Winter** | Cool | < 40 (deep) | > 35 (bright) |

Boundaries use **Gaussian fuzzy membership** rather than hard thresholds, so near-boundary colours receive a primary + secondary season and a calibrated confidence score (0–100%).

---

## PANTONE Data

> **IMPORTANT:** The PANTONE Fashion, Home + Interiors (FHI) palette is proprietary to Pantone LLC. The seed data bundled with this project (`PantoneSeedData.kt`) contains **representative approximate colours** for demonstration purposes only. For a commercial product, obtain a licensed dataset from [pantone.com](https://www.pantone.com) and replace `PantoneSeedData.raw` entries, or implement the optional Pantone Connect API integration.

### Pantone Connect API (optional)
Add your API token to `local.properties`:
```
pantone.connect.api.key=YOUR_TOKEN_HERE
```
The app reads this at runtime. All core functionality falls back to the local database when offline or when the token is absent.

---

## Season Classification Tuning

Thresholds live in `SeasonClassifier.SeasonThresholds` (a plain data class). Override them at runtime via:
```kotlin
val customThresholds = SeasonThresholds(
    lightL = 62.0,   // raise "light" cutoff for a stricter classification
    brightC = 40.0   // require more vivid colour for "bright" category
)
SeasonClassifier.classify(lab, thresholds = customThresholds)
```
Wire this to Remote Config / a debug settings screen for field calibration.

---

## Running the Tests

```bash
# Unit tests for colour science (no Android emulator needed)
./gradlew :core:core-color:test

# All tests
./gradlew test
```

The `DeltaE2000Test` validates all 34 test pairs from Sharma et al. 2005 (Table 1) to a tolerance of 0.005 ΔE — the accepted implementation verification standard.

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `CAMERA` | In-app fabric capture |
| `READ_MEDIA_IMAGES` | Gallery import (Android 13+) |
| `INTERNET` | Optional Pantone Connect API |

All image processing is **on-device**. Photos are never uploaded unless the user explicitly opts in.

---

## Colour Season Reference Card

| Season | L* | C* | Hue | Description |
|--------|----|----|-----|-------------|
| Spring | > 50 | > 35 | 15°–75° (warm) | Clear, warm, bright |
| Summer | > 50 | 15–35 | 165°–300° (cool) | Soft, cool, muted |
| Autumn | 30–65 | 15–35 | 15°–75° (warm) | Earthy, warm, muted |
| Winter | < 50 | > 35 | 165°–300° (cool) | Deep, cool, high contrast |
