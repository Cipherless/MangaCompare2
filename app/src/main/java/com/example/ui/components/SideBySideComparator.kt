package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComparisonImage
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun SideBySideComparator(
    images: List<ComparisonImage>,
    qualityRanks: Map<String, String>,
    onEditNote: (ComparisonImage) -> Unit,
    onRemoveImage: (ComparisonImage) -> Unit,
    onMoveLeft: (Int) -> Unit,
    onMoveRight: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (images.isEmpty()) return

    // Track scroll percent reported by each image
    val scrollPercentages = remember { mutableMapOf<Int, Float>() }
    var lastActiveIndex by remember { mutableIntStateOf(0) }
    var alignTriggerTarget by remember { mutableFloatStateOf(-1f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Layout 2 or more images side-by-side
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            images.forEachIndexed { index, image ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black)
                ) {
                    // Pinned Header Card with note, specs, reorder arrows, and 'x' button
                    ImageHeaderCard(
                        image = image,
                        qualityBadge = qualityRanks[image.id],
                        index = index,
                        totalCount = images.size,
                        onEditNote = { onEditNote(image) },
                        onRemove = { onRemoveImage(image) },
                        onMoveLeft = { onMoveLeft(index) },
                        onMoveRight = { onMoveRight(index) }
                    )

                    // Image viewer with pan, pinch-zoom, and scroll percent reporting
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        ManhwaImageViewer(
                            image = image,
                            alignPercentTarget = if (alignTriggerTarget >= 0f) alignTriggerTarget else null,
                            onScrollPercentChanged = { pct ->
                                scrollPercentages[index] = pct
                                lastActiveIndex = index
                            }
                        )
                    }
                }
            }
        }

        // Floating "Align %" Relative Position Match Button
        if (images.size > 1) {
            Button(
                onClick = {
                    val activePct = scrollPercentages[lastActiveIndex] ?: 0f
                    alignTriggerTarget = activePct
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .testTag("align_percent_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val currentPctDisplay = ((scrollPercentages[lastActiveIndex] ?: 0f) * 100).toInt()
                Text(
                    text = "Align to $currentPctDisplay%",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
