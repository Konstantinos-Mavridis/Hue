package com.hue.feature.history.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.hue.core.color.model.*
import com.hue.domain.model.*
import com.hue.domain.usecase.DeleteScanUseCase
import com.hue.domain.usecase.GetHistoryUseCase
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val mockGetHistory = mockk<GetHistoryUseCase>()
    private val mockDelete     = mockk<DeleteScanUseCase>()
    private lateinit var viewModel: HistoryViewModel

    private fun makeScan(id: Long, season: Season = Season.AUTUMN) = FabricAnalysis(
        id = id,
        timestamp = System.currentTimeMillis() - id * 1000,
        thumbnailPath = "/path/$id.jpg",
        dominantRgb = RgbColor(180, 90, 45),
        dominantLab = LabColor(52.0, 20.0, 30.0),
        topMatches = emptyList(),
        season = SeasonResult(
            primarySeason = season,
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

    @Before fun setup() {
        every { mockGetHistory() } returns flowOf(emptyList())
        viewModel = HistoryViewModel(mockGetHistory, mockDelete)
    }

    @After fun tearDown() { unmockkAll() }

    // ── Initial load ──────────────────────────────────────────────────────

    @Test fun `initial state has empty scans and isLoading false after load`() = runTest {
        every { mockGetHistory() } returns flowOf(emptyList())
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        assertThat(vm.uiState.value.scans).isEmpty()
        assertThat(vm.uiState.value.isLoading).isFalse()
    }

    @Test fun `scans are populated from repository`() = runTest {
        val scans = listOf(makeScan(1), makeScan(2), makeScan(3))
        every { mockGetHistory() } returns flowOf(scans)
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        assertThat(vm.uiState.value.scans).hasSize(3)
    }

    // ── Season filter ─────────────────────────────────────────────────────

    @Test fun `setFilter filters scans by season`() = runTest {
        val scans = listOf(
            makeScan(1, Season.AUTUMN),
            makeScan(2, Season.WINTER),
            makeScan(3, Season.AUTUMN)
        )
        every { mockGetHistory() } returns flowOf(scans)
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setFilter(Season.AUTUMN)

        assertThat(vm.uiState.value.filteredScans).hasSize(2)
        vm.uiState.value.filteredScans.forEach { scan ->
            assertThat(scan.season.primarySeason).isEqualTo(Season.AUTUMN)
        }
    }

    @Test fun `setFilter null returns all scans`() = runTest {
        val scans = listOf(makeScan(1, Season.SPRING), makeScan(2, Season.WINTER))
        every { mockGetHistory() } returns flowOf(scans)
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setFilter(Season.SPRING)
        vm.setFilter(null)

        assertThat(vm.uiState.value.filteredScans).hasSize(2)
    }

    @Test fun `setFilter with no matching season returns empty list`() = runTest {
        val scans = listOf(makeScan(1, Season.AUTUMN), makeScan(2, Season.SUMMER))
        every { mockGetHistory() } returns flowOf(scans)
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setFilter(Season.WINTER)
        assertThat(vm.uiState.value.filteredScans).isEmpty()
    }

    // ── Search query ──────────────────────────────────────────────────────

    @Test fun `setSearch with empty query returns all scans`() = runTest {
        val scans = listOf(makeScan(1), makeScan(2))
        every { mockGetHistory() } returns flowOf(scans)
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setSearch("")
        assertThat(vm.uiState.value.filteredScans).hasSize(2)
    }

    @Test fun `setSearch by season name filters correctly`() = runTest {
        val autumnScan  = makeScan(1, Season.AUTUMN)
        val winterScan  = makeScan(2, Season.WINTER)
        every { mockGetHistory() } returns flowOf(listOf(autumnScan, winterScan))
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setSearch("Autumn")
        assertThat(vm.uiState.value.filteredScans).hasSize(1)
        assertThat(vm.uiState.value.filteredScans[0].season.primarySeason).isEqualTo(Season.AUTUMN)
    }

    @Test fun `setSearch is case insensitive`() = runTest {
        val scan = makeScan(1, Season.SPRING)
        every { mockGetHistory() } returns flowOf(listOf(scan))
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setSearch("spring")
        assertThat(vm.uiState.value.filteredScans).hasSize(1)

        vm.setSearch("SPRING")
        assertThat(vm.uiState.value.filteredScans).hasSize(1)
    }

    // ── Filter + search combined ──────────────────────────────────────────

    @Test fun `filter and search are applied together`() = runTest {
        val scans = listOf(
            makeScan(1, Season.AUTUMN),
            makeScan(2, Season.AUTUMN),
            makeScan(3, Season.WINTER)
        )
        every { mockGetHistory() } returns flowOf(scans)
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.setFilter(Season.AUTUMN)
        vm.setSearch("Autumn")

        assertThat(vm.uiState.value.filteredScans).hasSize(2)
    }

    // ── delete ────────────────────────────────────────────────────────────

    @Test fun `delete calls DeleteScanUseCase with correct id`() = runTest {
        every { mockGetHistory() } returns flowOf(listOf(makeScan(10L)))
        coEvery { mockDelete(10L) } just Runs
        val vm = HistoryViewModel(mockGetHistory, mockDelete)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        vm.delete(10L)
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        coVerify { mockDelete(10L) }
    }
}
