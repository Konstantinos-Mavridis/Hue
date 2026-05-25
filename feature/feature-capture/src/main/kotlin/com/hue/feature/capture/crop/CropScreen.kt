package com.hue.feature.capture.crop

import android.graphics.RectF
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hue.core.design.theme.HueShapes

@Composable
fun CropScreen(
    imagePath: String,
    onCropConfirmed: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CropViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(imagePath) { viewModel.loadImage(imagePath) }
    LaunchedEffect(state.croppedPath) {
        state.croppedPath?.let { onCropConfirmed(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Region") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.confirmCrop() },
                        enabled = !state.isProcessing
                    ) {
                        Text("Analyse")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hint banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp),
                         tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "Drag handles to select a uniform fabric area. Avoid seams, shadows, and patterns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = state.bitmap
                if (bitmap != null) {
                    InteractiveCropView(
                        bitmap = bitmap,
                        cropRect = state.cropRect,
                        onCropRectChange = { viewModel.updateCropRect(it) }
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            // Warning snackbar
            AnimatedVisibility(visible = state.warning != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            state.warning?.message ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissWarning() }) {
                            Icon(Icons.Default.Close, "Dismiss",
                                 tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            if (state.isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun InteractiveCropView(
    bitmap: android.graphics.Bitmap,
    cropRect: RectF,
    onCropRectChange: (RectF) -> Unit
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var localRect by remember { mutableStateOf(cropRect) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .clip(HueShapes.medium)
            .onSizeChanged { imageSize = it }
    ) {
        // The image itself
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Fabric image for region selection",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // Crop overlay
        CropOverlay(
            cropRect = localRect,
            imageSize = imageSize,
            onRectChange = { newRect ->
                localRect = newRect
                onCropRectChange(newRect)
            }
        )
    }
}

@Composable
private fun CropOverlay(
    cropRect: RectF,
    imageSize: IntSize,
    onRectChange: (RectF) -> Unit
) {
    if (imageSize == IntSize.Zero) return

    val w = imageSize.width.toFloat()
    val h = imageSize.height.toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / w
                    val dy = dragAmount.y / h
                    val newRect = RectF(
                        (cropRect.left + dx).coerceIn(0f, cropRect.right - 0.1f),
                        (cropRect.top + dy).coerceIn(0f, cropRect.bottom - 0.1f),
                        (cropRect.right + dx).coerceIn(cropRect.left + 0.1f, 1f),
                        (cropRect.bottom + dy).coerceIn(cropRect.top + 0.1f, 1f)
                    )
                    onRectChange(newRect)
                }
            }
    ) {
        val left   = cropRect.left * w
        val top    = cropRect.top * h
        val right  = cropRect.right * w
        val bottom = cropRect.bottom * h

        // Dim mask
        drawRect(Color.Black.copy(alpha = 0.5f))
        // Clear crop area (approximate — just draw the border)
        drawRect(
            color = Color.White.copy(alpha = 0.12f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
        )
        // Border
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
            style = Stroke(width = 2.dp.toPx())
        )
        // Handles
        val hSize = 10.dp.toPx()
        listOf(
            Offset(left - hSize / 2, top - hSize / 2),
            Offset(right - hSize / 2, top - hSize / 2),
            Offset(left - hSize / 2, bottom - hSize / 2),
            Offset(right - hSize / 2, bottom - hSize / 2)
        ).forEach { pos ->
            drawRect(Color.White, topLeft = pos, size = androidx.compose.ui.geometry.Size(hSize, hSize))
        }
    }
}

private val CropWarning.message: String get() = when (this) {
    CropWarning.REGION_TOO_SMALL -> "Selected area is too small. Expand the crop region."
    CropWarning.REGION_TOO_NOISY -> "Complex pattern detected. Focus on a single-colour area."
    CropWarning.GLARE_DETECTED   -> "Glare detected. Try diffuse lighting or move to shade."
    CropWarning.LOW_LIGHT        -> "Image appears too dark. Try again in better light."
}
