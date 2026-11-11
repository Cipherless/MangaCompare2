package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComparisonImage
import com.example.ui.theme.AmberSecond
import com.example.ui.theme.CoralThird
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.GreenBest
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImageHeaderCard(
    image: ComparisonImage,
    qualityBadge: String?,
    index: Int,
    totalCount: Int,
    onEditNote: () -> Unit,
    onRemove: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceContainer.copy(alpha = 0.94f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Row: Source Note, Quality Chip, Reorder Controls, and 'x' Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source Note Tag (Clickable to Edit)
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onEditNote() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("edit_note_tag_${image.id}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (image.note.isNotBlank()) image.note else "Source #${index + 1}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit note",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Optional Quality Ranking Badge
                if (!qualityBadge.isNullOrBlank()) {
                    val badgeColor = when {
                        qualityBadge.contains("Best", ignoreCase = true) -> GreenBest
                        qualityBadge.contains("2nd", ignoreCase = true) -> AmberSecond
                        else -> CoralThird
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = qualityBadge,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Reorder arrows
                if (totalCount > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (index > 0) {
                            IconButton(
                                onClick = onMoveLeft,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("move_left_${image.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Move left",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < totalCount - 1) {
                            IconButton(
                                onClick = onMoveRight,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("move_right_${image.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Move right",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Dedicated small 'x' button to remove single image from comparison
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(alpha = 0.16f))
                        .clickable { onRemove() }
                        .testTag("remove_image_${image.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove image from comparison",
                        tint = ErrorRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom Row: Compact Technical Specs (Monospace numbers, readable metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Resolution
                Text(
                    text = "${image.width} × ${image.height}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
                Text("•", color = TextMuted, fontSize = 11.sp)

                // Format
                Text(
                    text = image.formatDisplayName,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text("•", color = TextMuted, fontSize = 11.sp)

                // File size
                Text(
                    text = image.formattedFileSize,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                )
                Text("•", color = TextMuted, fontSize = 11.sp)

                // Megapixels
                Text(
                    text = image.formattedMegapixels,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                )

                // Aspect ratio
                Text("•", color = TextMuted, fontSize = 11.sp)
                Text(
                    text = image.aspectRatioString,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                )
            }
        }
    }
}
