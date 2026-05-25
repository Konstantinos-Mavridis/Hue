package com.hue.feature.capture.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

enum class CaptureMode { HOME, CAMERA, LOADING }

data class CaptureUiState(
    val mode: CaptureMode = CaptureMode.HOME,
    val hasCameraPermission: Boolean = false,
    val capturedPath: String? = null,
    val error: String? = null
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CaptureUiState())
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    fun requestCamera() {
        _uiState.value = _uiState.value.copy(mode = CaptureMode.CAMERA)
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasCameraPermission = granted)
        if (!granted) {
            _uiState.value = _uiState.value.copy(
                error = "Camera permission denied. Please enable it in Settings."
            )
        }
    }

    fun onImageCaptured(path: String) {
        _uiState.value = _uiState.value.copy(capturedPath = path)
    }

    fun onGalleryImageSelected(uri: Uri, context: Context) {
        _uiState.value = _uiState.value.copy(mode = CaptureMode.LOADING)
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "gallery_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
                file.absolutePath
            }
            _uiState.value = _uiState.value.copy(
                mode = CaptureMode.HOME,
                capturedPath = path
            )
        }
    }

    fun onCaptureError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    fun goHome() {
        _uiState.value = _uiState.value.copy(mode = CaptureMode.HOME, capturedPath = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
