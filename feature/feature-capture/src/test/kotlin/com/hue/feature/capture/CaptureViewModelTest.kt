package com.hue.feature.capture

import com.google.common.truth.Truth.assertThat
import com.hue.feature.capture.ui.CaptureMode
import com.hue.feature.capture.ui.CaptureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import io.mockk.*
import android.content.Context

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureViewModelTest {

    private val testDispatcher = TestCoroutineDispatcher()
    private val mockContext    = mockk<Context>(relaxed = true)
    private lateinit var viewModel: CaptureViewModel

    @Before fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CaptureViewModel(mockContext)
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
        unmockkAll()
    }

    // ── Initial state ─────────────────────────────────────────────────────

    @Test fun `initial state is HOME mode`() {
        assertThat(viewModel.uiState.value.mode).isEqualTo(CaptureMode.HOME)
    }

    @Test fun `initial state has no captured path`() {
        assertThat(viewModel.uiState.value.capturedPath).isNull()
    }

    @Test fun `initial state has no error`() {
        assertThat(viewModel.uiState.value.error).isNull()
    }

    // ── requestCamera ─────────────────────────────────────────────────────

    @Test fun `requestCamera switches to CAMERA mode`() {
        viewModel.requestCamera()
        assertThat(viewModel.uiState.value.mode).isEqualTo(CaptureMode.CAMERA)
    }

    // ── goHome ────────────────────────────────────────────────────────────

    @Test fun `goHome resets to HOME mode`() {
        viewModel.requestCamera()
        viewModel.goHome()
        assertThat(viewModel.uiState.value.mode).isEqualTo(CaptureMode.HOME)
    }

    @Test fun `goHome clears captured path`() {
        viewModel.onImageCaptured("/path/img.jpg")
        viewModel.goHome()
        assertThat(viewModel.uiState.value.capturedPath).isNull()
    }

    // ── onCameraPermissionResult ──────────────────────────────────────────

    @Test fun `permission granted sets hasCameraPermission true`() {
        viewModel.onCameraPermissionResult(true)
        assertThat(viewModel.uiState.value.hasCameraPermission).isTrue()
    }

    @Test fun `permission denied sets hasCameraPermission false`() {
        viewModel.onCameraPermissionResult(false)
        assertThat(viewModel.uiState.value.hasCameraPermission).isFalse()
    }

    @Test fun `permission denied sets error message`() {
        viewModel.onCameraPermissionResult(false)
        assertThat(viewModel.uiState.value.error).isNotNull()
        assertThat(viewModel.uiState.value.error).isNotEmpty()
    }

    @Test fun `permission granted does not set error`() {
        viewModel.onCameraPermissionResult(true)
        assertThat(viewModel.uiState.value.error).isNull()
    }

    // ── onImageCaptured ───────────────────────────────────────────────────

    @Test fun `onImageCaptured stores the path`() {
        viewModel.onImageCaptured("/tmp/capture_123.jpg")
        assertThat(viewModel.uiState.value.capturedPath).isEqualTo("/tmp/capture_123.jpg")
    }

    // ── onCaptureError ────────────────────────────────────────────────────

    @Test fun `onCaptureError stores error message`() {
        viewModel.onCaptureError("Camera hardware not available")
        assertThat(viewModel.uiState.value.error).contains("Camera hardware not available")
    }

    // ── clearError ────────────────────────────────────────────────────────

    @Test fun `clearError removes the error`() {
        viewModel.onCaptureError("Some error")
        viewModel.clearError()
        assertThat(viewModel.uiState.value.error).isNull()
    }

    @Test fun `clearError when no error is a no-op`() {
        viewModel.clearError()
        assertThat(viewModel.uiState.value.error).isNull()
    }
}
