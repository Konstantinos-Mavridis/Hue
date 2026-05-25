package com.hue.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.hue.core.color.model.*
import com.hue.domain.model.*
import com.hue.domain.repository.HistoryRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class HistoryUseCasesTest {

    private val historyRepo = mockk<HistoryRepository>()

    @After fun tearDown() { unmockkAll() }

    private fun makeAnalysis(id: Long = 1L) = FabricAnalysis(
        id = id,
        timestamp = System.currentTimeMillis(),
        thumbnailPath = "/path/img.jpg",
        dominantRgb = RgbColor(180, 90, 45),
        dominantLab = LabColor(52.0, 20.0, 30.0),
        topMatches = emptyList(),
        season = SeasonResult(
            primarySeason = Season.AUTUMN,
            secondarySeason = null,
            primaryWeight = 0.8,
            temperature = ColorTemperature.WARM,
            confidence = 0.8,
            lightness = LightnessCategory.MEDIUM,
            chroma = ChromaCategory.MUTED,
            explanation = "Test"
        ),
        regionVariance = 50.0
    )

    // ── GetHistoryUseCase ─────────────────────────────────────────────────

    @Test fun `GetHistoryUseCase returns all scans from repository`() = runTest {
        val scans = listOf(makeAnalysis(1L), makeAnalysis(2L))
        every { historyRepo.getAllScans() } returns flowOf(scans)

        val useCase = GetHistoryUseCase(historyRepo)
        val result = useCase().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(1L)
        verify { historyRepo.getAllScans() }
    }

    @Test fun `GetHistoryUseCase returns empty list when no scans`() = runTest {
        every { historyRepo.getAllScans() } returns flowOf(emptyList())

        val useCase = GetHistoryUseCase(historyRepo)
        val result = useCase().first()

        assertThat(result).isEmpty()
    }

    // ── SaveScanUseCase ───────────────────────────────────────────────────

    @Test fun `SaveScanUseCase delegates to repository and returns id`() = runTest {
        val analysis = makeAnalysis()
        coEvery { historyRepo.saveScan(analysis) } returns 42L

        val useCase = SaveScanUseCase(historyRepo)
        val resultId = useCase(analysis)

        assertThat(resultId).isEqualTo(42L)
        coVerify { historyRepo.saveScan(analysis) }
    }

    @Test fun `SaveScanUseCase passes the full analysis object`() = runTest {
        val analysis = makeAnalysis(id = 99L)
        val slot = slot<FabricAnalysis>()
        coEvery { historyRepo.saveScan(capture(slot)) } returns 99L

        val useCase = SaveScanUseCase(historyRepo)
        useCase(analysis)

        assertThat(slot.captured.id).isEqualTo(99L)
        assertThat(slot.captured.thumbnailPath).isEqualTo("/path/img.jpg")
    }

    // ── DeleteScanUseCase ─────────────────────────────────────────────────

    @Test fun `DeleteScanUseCase calls repository deleteById`() = runTest {
        coEvery { historyRepo.deleteScan(5L) } just Runs

        val useCase = DeleteScanUseCase(historyRepo)
        useCase(5L)

        coVerify { historyRepo.deleteScan(5L) }
    }

    @Test fun `DeleteScanUseCase with different ids calls repo each time`() = runTest {
        coEvery { historyRepo.deleteScan(any()) } just Runs

        val useCase = DeleteScanUseCase(historyRepo)
        useCase(1L)
        useCase(2L)
        useCase(3L)

        coVerify(exactly = 1) { historyRepo.deleteScan(1L) }
        coVerify(exactly = 1) { historyRepo.deleteScan(2L) }
        coVerify(exactly = 1) { historyRepo.deleteScan(3L) }
    }

    // ── GetScanByIdUseCase ────────────────────────────────────────────────

    @Test fun `GetScanByIdUseCase returns scan when found`() = runTest {
        val analysis = makeAnalysis(7L)
        coEvery { historyRepo.getScanById(7L) } returns analysis

        val useCase = GetScanByIdUseCase(historyRepo)
        val result = useCase(7L)

        assertThat(result).isNotNull()
        assertThat(result!!.id).isEqualTo(7L)
    }

    @Test fun `GetScanByIdUseCase returns null when not found`() = runTest {
        coEvery { historyRepo.getScanById(999L) } returns null

        val useCase = GetScanByIdUseCase(historyRepo)
        assertThat(useCase(999L)).isNull()
    }
}
