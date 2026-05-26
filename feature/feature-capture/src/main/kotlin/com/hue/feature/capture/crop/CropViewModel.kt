package com.hue.feature.capture.crop

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
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

data class CropUiState(
    val sourcePath: String = "",
    val bitmap: Bitmap? = null,
    val cropRect: RectF = RectF(0.1f, 0.1f, 0.9f, 0.9f), // normalised 0..1
    val isProcessing: Boolean = false,
    val croppedPath: String? = null,
    val warning: CropWarning? = null,
    val error: String? = null
)

enum class CropWarning {
    REGION_TOO_SMALL, REGION_TOO_NOISY, GLARE_DETECTED, LOW_LIGHT
}

@HiltViewModel
class CropViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CropUiState())
    val uiState: StateFlow<CropUiState> = _uiState.asStateFlow()

    fun loadImage(path: String) {
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(path)
            }
            _uiState.value = _uiState.value.copy(sourcePath = path, bitmap = bitmap)
        }
    }

    fun updateCropRect(rect: RectF) {
        val w = rect.width(); val h = rect.height()
        val minDim = 0.05f  // at least 5% of image dimension
        if (w < minDim || h < minDim) {
            _uiState.value = _uiState.value.copy(warning = CropWarning.REGION_TOO_SMALL)
            return
        }
        _uiState.value = _uiState.value.copy(cropRect = rect, warning = null)
    }

    fun confirmCrop() {
        val state = _uiState.value
        val bitmap = state.bitmap ?: return
        _uiState.value = state.copy(isProcessing = true)

        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                val rect = state.cropRect
                val x = (rect.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                val y = (rect.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                val w = ((rect.right - rect.left) * bitmap.width).toInt().coerceAtLeast(100).coerceAtMost(bitmap.width - x)
                val h = ((rect.bottom - rect.top) * bitmap.height).toInt().coerceAtLeast(100).coerceAtMost(bitmap.height - y)

                val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                val file = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { cropped.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                file.absolutePath
            }
            _uiState.value = _uiState.value.copy(
                isProcessing = false,
                croppedPath = result
            )
        }
    }

    fun dismissWarning() {
        _uiState.value = _uiState.value.copy(warning = null)
    }
}
