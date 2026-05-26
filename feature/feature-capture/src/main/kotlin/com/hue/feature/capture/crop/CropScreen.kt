package com.hue.feature.capture.crop

import android.graphics.RectF
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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

private enum class DragZone { NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, MOVE }

private fun Offset.isNear(x: Float, y: Float, threshold: Float) =
    abs(this.x - x) <= threshold && abs(this.y - y) <= threshold

@Composable
private fun CropOverlay(
    cropRect: RectF,
    imageSize: IntSize,
    onRectChange: (RectF) -> Unit
) {
    if (imageSize == IntSize.Zero) return

    val w = imageSize.width.toFloat()
    val h = imageSize.height.toFloat()
    val density = LocalDensity.current
    val handleTouchPx = with(density) { 44.dp.toPx() }

    // Always read the latest rect inside gesture callbacks without restarting the effect.
    val currentRect by rememberUpdatedState(cropRect)
    var dragZone by remember { mutableStateOf(DragZone.NONE) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val r = currentRect
                        val left   = r.left * w
                        val top    = r.top * h
                        val right  = r.right * w
                        val bottom = r.bottom * h
                        // Corners take priority over interior.
                        dragZone = when {
                            offset.isNear(left,  top,    handleTouchPx) -> DragZone.TOP_LEFT
                            offset.isNear(right, top,    handleTouchPx) -> DragZone.TOP_RIGHT
                            offset.isNear(left,  bottom, handleTouchPx) -> DragZone.BOTTOM_LEFT
                            offset.isNear(right, bottom, handleTouchPx) -> DragZone.BOTTOM_RIGHT
                            offset.x in left..right && offset.y in top..bottom -> DragZone.MOVE
                            else -> DragZone.NONE
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val r = currentRect
                        val dx = dragAmount.x / w
                        val dy = dragAmount.y / h
                        val min = 0.1f
                        val newRect: RectF? = when (dragZone) {
                            DragZone.TOP_LEFT -> RectF(
                                (r.left + dx).coerceIn(0f, r.right - min),
                                (r.top  + dy).coerceIn(0f, r.bottom - min),
                                r.right, r.bottom
                            )
                            DragZone.TOP_RIGHT -> RectF(
                                r.left,
                                (r.top   + dy).coerceIn(0f, r.bottom - min),
                                (r.right + dx).coerceIn(r.left + min, 1f),
                                r.bottom
                            )
                            DragZone.BOTTOM_LEFT -> RectF(
                                (r.left   + dx).coerceIn(0f, r.right - min),
                                r.top, r.right,
                                (r.bottom + dy).coerceIn(r.top + min, 1f)
                            )
                            DragZone.BOTTOM_RIGHT -> RectF(
                                r.left, r.top,
                                (r.right  + dx).coerceIn(r.left + min, 1f),
                                (r.bottom + dy).coerceIn(r.top  + min, 1f)
                            )
                            DragZone.MOVE -> {
                                val rw = r.right - r.left
                                val rh = r.bottom - r.top
                                val newLeft = (r.left + dx).coerceIn(0f, 1f - rw)
                                val newTop  = (r.top  + dy).coerceIn(0f, 1f - rh)
                                RectF(newLeft, newTop, newLeft + rw, newTop + rh)
                            }
                            DragZone.NONE -> null
                        }
                        newRect?.let { onRectChange(it) }
                    }
                )
            }
    ) {
        val left   = cropRect.left * w
        val top    = cropRect.top * h
        val right  = cropRect.right * w
        val bottom = cropRect.bottom * h

        // Dim mask
        drawRect(Color.Black.copy(alpha = 0.5f))
        // Highlight crop area
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
        // Corner handles — 16 dp squares, centred on each corner
        val hSize = 16.dp.toPx()
        listOf(
            Offset(left  - hSize / 2, top    - hSize / 2),
            Offset(right - hSize / 2, top    - hSize / 2),
            Offset(left  - hSize / 2, bottom - hSize / 2),
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
