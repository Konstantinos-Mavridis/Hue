package com.hue.domain.usecase

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import com.hue.core.color.model.*
import com.hue.domain.model.*
import com.hue.domain.repository.PantoneRepository
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyseFabricUseCaseTest {

    private val mockRepo    = mockk<PantoneRepository>()
    private val mockContext = mockk<android.content.Context>(relaxed = true)
    private lateinit var useCase: AnalyseFabricUseCase

    @Before fun setup() {
        useCase = AnalyseFabricUseCase(mockRepo, mockContext)
    }

    @After fun tearDown() { unmockkAll() }

    private fun makeMatch(deltaE: Double = 2.0) = PantoneMatchResult(
        color = PantoneFhiColor(
            code = "18-1550 TCX", name = "Burnt Orange",
            lab  = LabColor(52.0, 28.0, 35.0), hex = "#C05C28",
            temperature = ColorTemperature.WARM,
            seasons = listOf(Season.AUTUMN)
        ),
        deltaE = deltaE
    )

    private fun orangeBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        repeat(10) { x -> repeat(10) { y ->
            bmp.setPixel(x, y, Color.argb(255, 180, 90, 45))
        }}
        return bmp
    }

    private fun grayBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        repeat(10) { x -> repeat(10) { y ->
            bmp.setPixel(x, y, Color.argb(255, 128, 128, 128))
        }}
        return bmp
    }

    private fun brightBitmap(): Bitmap {
        val bmp = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        repeat(10) { x -> repeat(10) { y ->
            bmp.setPixel(x, y, Color.argb(255, 240, 240, 240))
        }}
        return bmp
    }

    // ── Failure paths ─────────────────────────────────────────────────────────

    @Test fun `returns failure when bitmap cannot be decoded`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns null

        val result = useCase(AnalysisInput("/bad/path.jpg"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        assertThat(result.exceptionOrNull()!!.message).contains("Cannot decode bitmap")
    }

    @Test fun `returns failure when PANTONE database is empty for chromatic fabric`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns emptyList()

        val result = useCase(AnalysisInput("/path/img.jpg"))

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).contains("PANTONE database is empty")
    }

    @Test fun `wraps unexpected exception in Result failure`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } throws RuntimeException("IO error")

        val result = useCase(AnalysisInput("/path/img.jpg"))

        assertThat(result.isFailure).isTrue()
    }

    // ── Success paths ──────────────────────────────────────────────────────────

    @Test fun `returns success for chromatic fabric`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val result = useCase(AnalysisInput("/path/img.jpg"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test fun `analysis contains top matches from repository`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch(2.0), makeMatch(4.0))

        val analysis = useCase(AnalysisInput("/path/img.jpg")).getOrThrow()

        assertThat(analysis.topMatches).hasSize(2)
    }

    @Test fun `analysis thumbnail path matches input`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val analysis = useCase(AnalysisInput("/images/fabric_crop.jpg")).getOrThrow()

        assertThat(analysis.thumbnailPath).isEqualTo("/images/fabric_crop.jpg")
    }

    @Test fun `analysis regionVariance is non-negative`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val analysis = useCase(AnalysisInput("/path/img.jpg")).getOrThrow()

        assertThat(analysis.regionVariance).isAtLeast(0.0)
    }

    @Test fun `analysis dominantRgb is populated`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val analysis = useCase(AnalysisInput("/path/img.jpg")).getOrThrow()

        val rgb = analysis.dominantRgb
        assertThat(rgb.r).isIn(0..255)
        assertThat(rgb.g).isIn(0..255)
        assertThat(rgb.b).isIn(0..255)
    }

    @Test fun `analysis season is classified`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val analysis = useCase(AnalysisInput("/path/img.jpg")).getOrThrow()

        assertThat(analysis.season.primarySeason).isNotNull()
    }

    @Test fun `analysis timestamp is set`() = runTest {
        val before = System.currentTimeMillis()
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns orangeBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val analysis = useCase(AnalysisInput("/path/img.jpg")).getOrThrow()

        assertThat(analysis.timestamp).isAtLeast(before)
    }

    // ── Achromatic path ────────────────────────────────────────────────────────

    @Test fun `returns success for achromatic fabric (early return path)`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns grayBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val result = useCase(AnalysisInput("/path/gray.jpg"))

        assertThat(result.isSuccess).isTrue()
    }

    @Test fun `achromatic fabric returns analysis even with empty matches`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns grayBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns emptyList()

        val result = useCase(AnalysisInput("/path/gray.jpg"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().topMatches).isEmpty()
    }

    // ── Illuminant / midtone path ──────────────────────────────────────────────

    @Test fun `bright fabric still produces a valid analysis`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns brightBitmap()
        coEvery { mockRepo.findTopMatches(any(), any()) } returns listOf(makeMatch())

        val result = useCase(AnalysisInput("/path/bright.jpg"))

        assertThat(result.isSuccess).isTrue()
    }
}
