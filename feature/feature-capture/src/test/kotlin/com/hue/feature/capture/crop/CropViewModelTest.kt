package com.hue.feature.capture.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class CropViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockContext    = mockk<Context>(relaxed = true)
    private lateinit var viewModel: CropViewModel

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { mockContext.cacheDir } returns File(System.getProperty("java.io.tmpdir")!!)
        viewModel = CropViewModel(mockContext)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ── Initial state ──────────────────────────────────────────────────────────

    @Test fun `initial bitmap is null`() {
        assertThat(viewModel.uiState.value.bitmap).isNull()
    }

    @Test fun `initial warning is null`() {
        assertThat(viewModel.uiState.value.warning).isNull()
    }

    @Test fun `initial croppedPath is null`() {
        assertThat(viewModel.uiState.value.croppedPath).isNull()
    }

    @Test fun `initial isProcessing is false`() {
        assertThat(viewModel.uiState.value.isProcessing).isFalse()
    }

    @Test fun `initial cropRect has 0_1 to 0_9 bounds`() {
        val rect = viewModel.uiState.value.cropRect
        assertThat(rect.left).isWithin(0.001f).of(0.1f)
        assertThat(rect.top).isWithin(0.001f).of(0.1f)
        assertThat(rect.right).isWithin(0.001f).of(0.9f)
        assertThat(rect.bottom).isWithin(0.001f).of(0.9f)
    }

    // ── updateCropRect ─────────────────────────────────────────────────────────

    @Test fun `updateCropRect with valid rect updates state`() {
        val newRect = RectF(0.2f, 0.2f, 0.8f, 0.8f)
        viewModel.updateCropRect(newRect)
        assertThat(viewModel.uiState.value.cropRect).isEqualTo(newRect)
    }

    @Test fun `updateCropRect with valid rect clears existing warning`() {
        viewModel.updateCropRect(RectF(0.1f, 0.1f, 0.13f, 0.9f)) // sets warning
        viewModel.updateCropRect(RectF(0.1f, 0.1f, 0.9f, 0.9f))  // valid
        assertThat(viewModel.uiState.value.warning).isNull()
    }

    @Test fun `updateCropRect with too-narrow rect sets REGION_TOO_SMALL`() {
        viewModel.updateCropRect(RectF(0.1f, 0.1f, 0.13f, 0.9f)) // width 0.03 < 0.05
        assertThat(viewModel.uiState.value.warning).isEqualTo(CropWarning.REGION_TOO_SMALL)
    }

    @Test fun `updateCropRect with too-short rect sets REGION_TOO_SMALL`() {
        viewModel.updateCropRect(RectF(0.1f, 0.1f, 0.9f, 0.12f)) // height 0.02 < 0.05
        assertThat(viewModel.uiState.value.warning).isEqualTo(CropWarning.REGION_TOO_SMALL)
    }

    @Test fun `updateCropRect with too-small rect does not update cropRect`() {
        val original = viewModel.uiState.value.cropRect
        viewModel.updateCropRect(RectF(0.1f, 0.1f, 0.13f, 0.9f))
        assertThat(viewModel.uiState.value.cropRect).isEqualTo(original)
    }

    @Test fun `updateCropRect at exact minimum dimensions is valid`() {
        // width = 0.9 - 0.85 = 0.05 exactly (not < 0.05), same for height
        val minRect = RectF(0.1f, 0.1f, 0.15f, 0.15f)
        viewModel.updateCropRect(minRect)
        assertThat(viewModel.uiState.value.warning).isNull()
        assertThat(viewModel.uiState.value.cropRect).isEqualTo(minRect)
    }

    @Test fun `multiple valid updateCropRect calls each update state`() {
        val rect1 = RectF(0.1f, 0.1f, 0.5f, 0.5f)
        val rect2 = RectF(0.2f, 0.2f, 0.8f, 0.8f)
        viewModel.updateCropRect(rect1)
        viewModel.updateCropRect(rect2)
        assertThat(viewModel.uiState.value.cropRect).isEqualTo(rect2)
    }

    // ── dismissWarning ─────────────────────────────────────────────────────────

    @Test fun `dismissWarning clears REGION_TOO_SMALL warning`() {
        viewModel.updateCropRect(RectF(0.1f, 0.1f, 0.13f, 0.9f))
        viewModel.dismissWarning()
        assertThat(viewModel.uiState.value.warning).isNull()
    }

    @Test fun `dismissWarning when no warning is a no-op`() {
        viewModel.dismissWarning()
        assertThat(viewModel.uiState.value.warning).isNull()
    }

    // ── confirmCrop ────────────────────────────────────────────────────────────

    @Test fun `confirmCrop with null bitmap is a no-op`() = runTest {
        viewModel.confirmCrop()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.croppedPath).isNull()
        assertThat(viewModel.uiState.value.isProcessing).isFalse()
    }

    @Test fun `confirmCrop sets isProcessing then resolves`() = runTest {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns bmp

        viewModel.loadImage("/path/img.jpg")
        advanceUntilIdle()

        viewModel.confirmCrop()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isProcessing).isFalse()
    }

    @Test fun `confirmCrop with valid bitmap produces a croppedPath`() = runTest {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns bmp

        viewModel.loadImage("/path/img.jpg")
        advanceUntilIdle()

        viewModel.confirmCrop()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.croppedPath).isNotNull()
    }

    @Test fun `confirmCrop croppedPath is a jpg file path`() = runTest {
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns bmp

        viewModel.loadImage("/path/img.jpg")
        advanceUntilIdle()
        viewModel.confirmCrop()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.croppedPath).endsWith(".jpg")
    }

    // ── loadImage ──────────────────────────────────────────────────────────────

    @Test fun `loadImage stores null bitmap when file not found`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile("/missing.jpg") } returns null

        viewModel.loadImage("/missing.jpg")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bitmap).isNull()
        assertThat(viewModel.uiState.value.sourcePath).isEqualTo("/missing.jpg")
    }

    @Test fun `loadImage sets sourcePath`() = runTest {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns null

        viewModel.loadImage("/some/path/fabric.jpg")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.sourcePath).isEqualTo("/some/path/fabric.jpg")
    }

    @Test fun `loadImage stores returned bitmap in state`() = runTest {
        val bmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeFile(any()) } returns bmp

        viewModel.loadImage("/path/img.jpg")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.bitmap).isEqualTo(bmp)
    }
}
