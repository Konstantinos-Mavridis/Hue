package com.hue.feature.matching.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.hue.core.color.model.*
import com.hue.domain.model.*
import com.hue.domain.usecase.AnalyseFabricUseCase
import com.hue.domain.usecase.SaveScanUseCase
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ResultsViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val mockAnalyse = mockk<AnalyseFabricUseCase>()
    private val mockSave    = mockk<SaveScanUseCase>()
    private lateinit var viewModel: ResultsViewModel

    private fun makeAnalysis() = FabricAnalysis(
        id = 0L,
        timestamp = System.currentTimeMillis(),
        thumbnailPath = "/tmp/test.jpg",
        dominantRgb = RgbColor(180, 90, 45),
        dominantLab = LabColor(52.0, 20.0, 30.0),
        topMatches = listOf(
            PantoneMatchResult(
                color = PantoneFhiColor(
                    code = "18-1550 TCX", name = "Burnt Orange",
                    lab = LabColor(52.0, 28.0, 35.0), hex = "#C05C28",
                    temperature = ColorTemperature.WARM,
                    seasons = listOf(Season.AUTUMN)
                ),
                deltaE = 2.1
            ),
            PantoneMatchResult(
                color = PantoneFhiColor(
                    code = "17-1344 TCX", name = "Amber",
                    lab = LabColor(60.0, 22.0, 45.0), hex = "#CC8040",
                    temperature = ColorTemperature.WARM,
                    seasons = listOf(Season.AUTUMN, Season.SPRING)
                ),
                deltaE = 3.5
            )
        ),
        season = SeasonResult(
            primarySeason = Season.AUTUMN,
            secondarySeason = Season.SPRING,
            primaryWeight = 0.75,
            temperature = ColorTemperature.WARM,
            confidence = 0.75,
            lightness = LightnessCategory.MEDIUM,
            chroma = ChromaCategory.MUTED,
            explanation = "This is an Autumn colour."
        ),
        regionVariance = 55.0
    )

    @Before fun setup() {
        viewModel = ResultsViewModel(mockAnalyse, mockSave, SavedStateHandle())
    }

    @After fun tearDown() { unmockkAll() }

    // ── Initial state ─────────────────────────────────────────────────────

    @Test fun `initial state is Idle`() {
        assertThat(viewModel.uiState.value).isInstanceOf(ResultsUiState.Idle::class.java)
    }

    // ── analyse ───────────────────────────────────────────────────────────

    @Test fun `analyse emits Loading then Success on success`() = runTest {
        val analysis = makeAnalysis()
        coEvery { mockAnalyse(any()) } returns Result.success(analysis)

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(ResultsUiState.Idle::class.java)
            viewModel.analyse("/tmp/test.jpg")
            assertThat(awaitItem()).isInstanceOf(ResultsUiState.Loading::class.java)
            val success = awaitItem()
            assertThat(success).isInstanceOf(ResultsUiState.Success::class.java)
            success as ResultsUiState.Success
            assertThat(success.analysis).isEqualTo(analysis)
            assertThat(success.selectedMatchIndex).isEqualTo(0)
            assertThat(success.isSaved).isFalse()
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `analyse emits Error on failure`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.failure(RuntimeException("Failed"))

        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(ResultsUiState.Idle::class.java)
            viewModel.analyse("/bad/path.jpg")
            assertThat(awaitItem()).isInstanceOf(ResultsUiState.Loading::class.java)
            val error = awaitItem()
            assertThat(error).isInstanceOf(ResultsUiState.Error::class.java)
            error as ResultsUiState.Error
            assertThat(error.message).contains("Failed")
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test fun `analyse error message has fallback text`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.failure(RuntimeException())

        viewModel.analyse("/path.jpg")
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(ResultsUiState.Error::class.java)
        state as ResultsUiState.Error
        assertThat(state.message).isNotEmpty()
    }

    // ── selectMatch ───────────────────────────────────────────────────────

    @Test fun `selectMatch updates selectedMatchIndex`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.success(makeAnalysis())
        viewModel.analyse("/tmp/test.jpg")
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        viewModel.selectMatch(1)

        val state = viewModel.uiState.value as ResultsUiState.Success
        assertThat(state.selectedMatchIndex).isEqualTo(1)
    }

    @Test fun `selectMatch on non-Success state is a no-op`() {
        viewModel.selectMatch(1)  // state is Idle — should not crash
        assertThat(viewModel.uiState.value).isInstanceOf(ResultsUiState.Idle::class.java)
    }

    // ── toggleAdvanced ────────────────────────────────────────────────────

    @Test fun `toggleAdvanced flips showAdvanced flag`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.success(makeAnalysis())
        viewModel.analyse("/tmp/test.jpg")
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        val before = (viewModel.uiState.value as ResultsUiState.Success).showAdvanced
        viewModel.toggleAdvanced()
        val after = (viewModel.uiState.value as ResultsUiState.Success).showAdvanced
        assertThat(after).isEqualTo(!before)
    }

    @Test fun `toggleAdvanced twice returns to original state`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.success(makeAnalysis())
        viewModel.analyse("/tmp/test.jpg")
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        viewModel.toggleAdvanced()
        viewModel.toggleAdvanced()
        assertThat((viewModel.uiState.value as ResultsUiState.Success).showAdvanced).isFalse()
    }

    // ── saveToHistory ─────────────────────────────────────────────────────

    @Test fun `saveToHistory marks result as saved`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.success(makeAnalysis())
        coEvery { mockSave(any()) } returns 1L
        viewModel.analyse("/tmp/test.jpg")
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        viewModel.saveToHistory()
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        assertThat((viewModel.uiState.value as ResultsUiState.Success).isSaved).isTrue()
    }

    @Test fun `saveToHistory is idempotent - only saves once`() = runTest {
        coEvery { mockAnalyse(any()) } returns Result.success(makeAnalysis())
        coEvery { mockSave(any()) } returns 1L
        viewModel.analyse("/tmp/test.jpg")
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        viewModel.saveToHistory()
        viewModel.saveToHistory()
        viewModel.saveToHistory()
        mainDispatcherRule.testDispatcher.advanceUntilIdle()

        coVerify(exactly = 1) { mockSave(any()) }
    }
}
