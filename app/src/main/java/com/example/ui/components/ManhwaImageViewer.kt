package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.ComparisonImage
import java.io.File
import kotlin.math.roundToInt

@Composable
fun ManhwaImageViewer(
    image: ComparisonImage,
    modifier: Modifier = Modifier,
    alignPercentTarget: Float? = null,
    onScrollPercentChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Zoom and Pan transform states
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
    ) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        // Approximate rendered height of the image based on aspect ratio
        val renderedImageHeightPx = if (image.width > 0) {
            containerWidthPx * (image.height.toFloat() / image.width.toFloat())
        } else {
            containerHeightPx
        }

        val maxScrollY = (renderedImageHeightPx * scale - containerHeightPx).coerceAtLeast(0f)
        val maxPanX = ((containerWidthPx * scale) - containerWidthPx).coerceAtLeast(0f) / 2f

        // Handle alignment target when "Align %" is pressed
        LaunchedEffect(alignPercentTarget) {
            if (alignPercentTarget != null && maxScrollY > 0f) {
                offsetY = - (alignPercentTarget * maxScrollY)
                onScrollPercentChanged?.invoke(alignPercentTarget)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(image.id) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 8f)

                        // If zoomed in, allow panning horizontally
                        val allowedPanX = ((containerWidthPx * scale) - containerWidthPx).coerceAtLeast(0f) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-allowedPanX, allowedPanX)

                        // Vertical scrolling / panning
                        val dynamicMaxScrollY = (renderedImageHeightPx * scale - containerHeightPx).coerceAtLeast(0f)
                        if (dynamicMaxScrollY > 0f) {
                            offsetY = (offsetY + pan.y).coerceIn(-dynamicMaxScrollY, 0f)
                            val currentPercent = (-offsetY / dynamicMaxScrollY).coerceIn(0f, 1f)
                            onScrollPercentChanged?.invoke(currentPercent)
                        } else {
                            offsetY = 0f
                            onScrollPercentChanged?.invoke(0f)
                        }
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(image.localFilePath))
                    .crossfade(true)
                    .build(),
                contentDescription = "Comparison image ${image.note}",
                contentScale = ContentScale.FillWidth,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
            )
        }
    }
}
