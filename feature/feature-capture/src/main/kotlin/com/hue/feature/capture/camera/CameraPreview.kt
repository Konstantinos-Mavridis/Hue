package com.hue.feature.capture.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onImageCaptured: (String) -> Unit,
    onError: (Exception) -> Unit,
    captureController: CaptureController
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(Unit) {
        val cameraProvider = context.getCameraProvider()
        val preview = Preview.Builder()
            .setTargetResolution(Size(1080, 1440))
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(Size(1080, 1440))
            .build()

        imageCaptureUseCase = imageCapture
        captureController.bind(imageCapture, context.mainExecutor, onImageCaptured, onError)

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageCapture
        )
    }

    Box(modifier = modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        GuidanceOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun GuidanceOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val boxSize = size.width * 0.6f
        val left  = centerX - boxSize / 2f
        val top   = centerY - boxSize / 2f

        // Dim surrounding area
        drawRect(Color.Black.copy(alpha = 0.35f))

        // Cut-out frame
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(boxSize, boxSize)
        )
        // Corner markers
        val cornerLen = 32.dp.toPx()
        val strokeW = 2.dp.toPx()
        val corners = listOf(
            Offset(left, top) to listOf(Offset(left + cornerLen, top), Offset(left, top + cornerLen)),
            Offset(left + boxSize, top) to listOf(Offset(left + boxSize - cornerLen, top), Offset(left + boxSize, top + cornerLen)),
            Offset(left, top + boxSize) to listOf(Offset(left + cornerLen, top + boxSize), Offset(left, top + boxSize - cornerLen)),
            Offset(left + boxSize, top + boxSize) to listOf(Offset(left + boxSize - cornerLen, top + boxSize), Offset(left + boxSize, top + boxSize - cornerLen))
        )
        corners.forEach { (start, ends) ->
            ends.forEach { end ->
                drawLine(Color.White, start, end, strokeWidth = strokeW)
            }
        }

        // Grid lines
        val third = boxSize / 3f
        val gridAlpha = 0.3f
        repeat(2) { i ->
            val x = left + third * (i + 1)
            val y = top + third * (i + 1)
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(x, top), Offset(x, top + boxSize), strokeWidth = 0.5.dp.toPx())
            drawLine(Color.White.copy(alpha = gridAlpha), Offset(left, y), Offset(left + boxSize, y), strokeWidth = 0.5.dp.toPx())
        }
    }
}

class CaptureController {
    private var imageCapture: ImageCapture? = null
    private var executor: Executor? = null
    private var onCaptured: ((String) -> Unit)? = null
    private var onError: ((Exception) -> Unit)? = null

    fun bind(
        capture: ImageCapture,
        executor: Executor,
        onCaptured: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        imageCapture = capture
        this.executor = executor
        this.onCaptured = onCaptured
        this.onError = onError
    }

    fun capture(outputFile: java.io.File) {
        val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture?.takePicture(
            options,
            executor ?: return,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onCaptured?.invoke(outputFile.absolutePath)
                }
                override fun onError(exception: ImageCaptureException) {
                    onError?.invoke(exception)
                }
            }
        )
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        ProcessCameraProvider.getInstance(this).also { future ->
            future.addListener({ cont.resume(future.get()) {} }, ContextCompat.getMainExecutor(this))
        }
    }
}
