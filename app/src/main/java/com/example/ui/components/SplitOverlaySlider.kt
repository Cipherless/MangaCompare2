package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.ComparisonImage
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceContainer
import java.io.File
import kotlin.math.roundToInt

@Composable
fun SplitOverlaySlider(
    images: List<ComparisonImage>,
    qualityRanks: Map<String, String>,
    sliderPair: Pair<Int, Int>,
    onPairChanged: (Int, Int) -> Unit,
    onSwapLayers: () -> Unit,
    onEditNote: (ComparisonImage) -> Unit,
    onRemoveImage: (ComparisonImage) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.size < 2) return

    val context = LocalContext.current
    val density = LocalDensity.current

    val firstIndex = sliderPair.first.coerceIn(0, images.size - 1)
    val secondIndex = sliderPair.second.coerceIn(0, images.size - 1)

    val imageA = images[firstIndex]
    val imageB = images[secondIndex]

    // Divider position 0.0 to 1.0 (defaults to 0.5 center)
    var dividerFraction by remember { mutableFloatStateOf(0.5f) }

    // Synchronized pan and zoom across both layers
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Headers for Image A and Image B
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ImageHeaderCard(
                    image = imageA,
                    qualityBadge = qualityRanks[imageA.id],
                    index = firstIndex,
                    totalCount = images.size,
                    onEditNote = { onEditNote(imageA) },
                    onRemove = { onRemoveImage(imageA) },
                    onMoveLeft = {},
                    onMoveRight = {}
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                ImageHeaderCard(
                    image = imageB,
                    qualityBadge = qualityRanks[imageB.id],
                    index = secondIndex,
                    totalCount = images.size,
                    onEditNote = { onEditNote(imageB) },
                    onRemove = { onRemoveImage(imageB) },
                    onMoveLeft = {},
                    onMoveRight = {}
                )
            }
        }

        // Main Overlay Canvas with draggable split divider
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Black)
                .clipToBounds()
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }

            // Estimate manhwa scroll limits
            val maxScrollY = ((heightPx * scale) - heightPx).coerceAtLeast(0f)
            val maxPanX = ((widthPx * scale) - widthPx).coerceAtLeast(0f) / 2f

            val dividerPx = widthPx * dividerFraction

            // Gesture detector for synchronized zoom and pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageA.id, imageB.id) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 10f)
                            offsetX = (offsetX + pan.x).coerceIn(-maxPanX * scale, maxPanX * scale)
                            offsetY = (offsetY + pan.y).coerceIn(-maxScrollY * scale, maxScrollY * scale)
                        }
                    }
            ) {
                // Layer 2: Right / Bottom Image (Revealed on right side of divider)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(imageB.localFilePath))
                        .crossfade(false)
                        .build(),
                    contentDescription = "Image B",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )

                // Layer 1: Left / Top Image (Clipped at divider line)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            clipRect(
                                left = 0f,
                                top = 0f,
                                right = dividerPx,
                                bottom = size.height
                            ) {
                                this@drawWithContent.drawContent()
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(File(imageA.localFilePath))
                            .crossfade(false)
                            .build(),
                        contentDescription = "Image A",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .offset { IntOffset(dividerPx.roundToInt() - 2.dp.roundToPx(), 0) }
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(CyanAccent)
                )

                // Draggable Thumb Handle
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dividerPx - 20.dp.toPx()).roundToInt(),
                                ((heightPx / 2f) - 20.dp.toPx()).roundToInt()
                            )
                        }
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CyanAccent)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newFraction = ((dividerPx + dragAmount.x) / widthPx).coerceIn(0.05f, 0.95f)
                                dividerFraction = newFraction
                            }
                        }
                        .testTag("slider_thumb_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Drag to compare",
                        tint = Color(0xFF002228),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bottom Controls: Swap Button & Image Selector
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DarkSurfaceContainer,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Swap Layers Button
                Button(
                    onClick = onSwapLayers,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("swap_layers_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Swap Top / Bottom", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Multiple-image Pair Picker
                if (images.size > 2) {
                    ImagePairDropdown(
                        images = images,
                        firstIndex = firstIndex,
                        secondIndex = secondIndex,
                        onPairChanged = onPairChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePairDropdown(
    images: List<ComparisonImage>,
    firstIndex: Int,
    secondIndex: Int,
    onPairChanged: (Int, Int) -> Unit
) {
    var expandedA by remember { mutableStateOf(false) }
    var expandedB by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Compare:", color = Color.Gray, fontSize = 11.sp)

        // Dropdown A
        Box {
            OutlinedButton(
                onClick = { expandedA = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = images[firstIndex].note.ifBlank { "Img #${firstIndex + 1}" },
                    fontSize = 11.sp
                )
            }
            DropdownMenu(
                expanded = expandedA,
                onDismissRequest = { expandedA = false }
            ) {
                images.forEachIndexed { idx, img ->
                    if (idx != secondIndex) {
                        DropdownMenuItem(
                            text = { Text(img.note.ifBlank { "Image #${idx + 1}" }) },
                            onClick = {
                                onPairChanged(idx, secondIndex)
                                expandedA = false
                            }
                        )
                    }
                }
            }
        }

        Text("vs", color = Color.Gray, fontSize = 11.sp)

        // Dropdown B
        Box {
            OutlinedButton(
                onClick = { expandedB = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = images[secondIndex].note.ifBlank { "Img #${secondIndex + 1}" },
                    fontSize = 11.sp
                )
            }
            DropdownMenu(
                expanded = expandedB,
                onDismissRequest = { expandedB = false }
            ) {
                images.forEachIndexed { idx, img ->
                    if (idx != firstIndex) {
                        DropdownMenuItem(
                            text = { Text(img.note.ifBlank { "Image #${idx + 1}" }) },
                            onClick = {
                                onPairChanged(firstIndex, idx)
                                expandedB = false
                            }
                        )
                    }
                }
            }
        }
    }
}
