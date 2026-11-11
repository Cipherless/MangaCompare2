package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.metadata.MetadataExtractor
import com.example.data.model.ComparisonImage
import com.example.ui.components.HistoryDialog
import com.example.ui.components.ImageHeaderCard
import com.example.ui.components.ManhwaImageViewer
import com.example.ui.components.NoteEditDialog
import com.example.ui.components.SideBySideComparator
import com.example.ui.components.SplitOverlaySlider
import com.example.ui.components.StorageSettingsDialog
import com.example.ui.components.TechnicalDetailsDialog
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GreenBest
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ComparisonMode
import com.example.ui.viewmodel.ComparisonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen(
    viewModel: ComparisonViewModel,
    modifier: Modifier = Modifier
) {
    val images by viewModel.currentImages.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val mode by viewModel.comparisonMode.collectAsStateWithLifecycle()
    val sliderPair by viewModel.sliderPair.collectAsStateWithLifecycle()
    val currentSessionId by viewModel.currentSessionId.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val storageUsedBytes by viewModel.storageUsedBytes.collectAsStateWithLifecycle()
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()

    // Dialog states
    var editingImageForNote by remember { mutableStateOf<ComparisonImage?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showTechDetailsDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // System Photo Picker (Zero-permission manual import)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris, defaultNotePrefix = "Imported")
        }
    }

    val qualityRanks = remember(images) {
        MetadataExtractor.computeQualityRanks(images)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "MangaCompare",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        if (images.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyanAccent.copy(alpha = 0.2f),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "${images.size} pages",
                                    color = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Technical Details Breakdown
                    if (images.isNotEmpty()) {
                        IconButton(
                            onClick = { showTechDetailsDialog = true },
                            modifier = Modifier.testTag("tech_details_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Technical details",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Comparison History
                    IconButton(
                        onClick = { showHistoryDialog = true },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = TextSecondary
                        )
                    }

                    // Privacy & Storage Settings
                    IconButton(
                        onClick = { showStorageDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Storage and Privacy",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            // Action Dock: Comparison Mode Switcher + Paste + Add Photo
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurface,
                tonalElevation = 6.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Comparison Mode Toggle (Side-by-Side vs Slider)
                        if (images.size >= 2) {
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.weight(1f)
                            ) {
                                SegmentedButton(
                                    selected = mode == ComparisonMode.SIDE_BY_SIDE,
                                    onClick = { viewModel.setComparisonMode(ComparisonMode.SIDE_BY_SIDE) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.ViewColumn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("mode_side_by_side")
                                ) {
                                    Text("Side-by-Side", fontSize = 12.sp)
                                }

                                SegmentedButton(
                                    selected = mode == ComparisonMode.OVERLAY_SLIDER,
                                    onClick = { viewModel.setComparisonMode(ComparisonMode.OVERLAY_SLIDER) },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Layers,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("mode_overlay_slider")
                                ) {
                                    Text("Slider", fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Dedicated Clipboard Paste Button
                        Button(
                            onClick = { viewModel.pasteFromClipboard() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanAccent,
                                contentColor = Color(0xFF002228)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("paste_clipboard_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Manual Image Picker Button
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .size(40.dp)
                                .testTag("pick_image_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Add image from device",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 0 Images: Rich Onboarding / Guidance
                images.isEmpty() -> {
                    EmptyComparisonState(
                        onPaste = { viewModel.pasteFromClipboard() },
                        onPick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }

                // 1 Image: Prompt user to share/paste 2nd image
                images.size == 1 -> {
                    val singleImg = images.first()
                    Column(modifier = Modifier.fillMaxSize()) {
                        ImageHeaderCard(
                            image = singleImg,
                            qualityBadge = null,
                            index = 0,
                            totalCount = 1,
                            onEditNote = { editingImageForNote = singleImg },
                            onRemove = { viewModel.removeSingleImage(singleImg) },
                            onMoveLeft = {},
                            onMoveRight = {}
                        )

                        Box(modifier = Modifier.weight(1f)) {
                            ManhwaImageViewer(image = singleImg)

                            // Overlay hint
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer.copy(alpha = 0.92f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = CyanAccent,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "1 page loaded from ${singleImg.note.ifBlank { "Source 1" }}",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Open the same page from another source in your reader or browser, and Share to MangaCompare (or tap Paste) to start side-by-side comparison!",
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2+ Images: Dual Mode Comparator
                else -> {
                    if (mode == ComparisonMode.SIDE_BY_SIDE) {
                        SideBySideComparator(
                            images = images,
                            qualityRanks = qualityRanks,
                            onEditNote = { editingImageForNote = it },
                            onRemoveImage = { viewModel.removeSingleImage(it) },
                            onMoveLeft = { idx -> viewModel.moveImage(idx, idx - 1) },
                            onMoveRight = { idx -> viewModel.moveImage(idx, idx + 1) }
                        )
                    } else {
                        SplitOverlaySlider(
                            images = images,
                            qualityRanks = qualityRanks,
                            sliderPair = sliderPair,
                            onPairChanged = { a, b -> viewModel.setSliderPair(a, b) },
                            onSwapLayers = { viewModel.swapSliderImages() },
                            onEditNote = { editingImageForNote = it },
                            onRemoveImage = { viewModel.removeSingleImage(it) }
                        )
                    }
                }
            }
        }
    }

    // Note Editor Dialog
    editingImageForNote?.let { targetImage ->
        NoteEditDialog(
            image = targetImage,
            onDismiss = { editingImageForNote = null },
            onSaveNote = { newNote ->
                viewModel.updateNote(targetImage.id, newNote)
            }
        )
    }

    // History Dialog
    if (showHistoryDialog) {
        HistoryDialog(
            sessions = sessions,
            currentSessionId = currentSessionId,
            onLoadSession = { viewModel.loadSession(it) },
            onDeleteSession = { viewModel.deleteSession(it) },
            onDismiss = { showHistoryDialog = false }
        )
    }

    // Storage and Privacy Settings Dialog
    if (showStorageDialog) {
        StorageSettingsDialog(
            storageUsedBytes = storageUsedBytes,
            retentionDays = retentionDays,
            onSelectRetentionDays = { viewModel.setRetentionDays(it) },
            onClearAllData = { viewModel.clearAll() },
            onDismiss = { showStorageDialog = false }
        )
    }

    // Technical Specs Breakdown Dialog
    if (showTechDetailsDialog && images.isNotEmpty()) {
        TechnicalDetailsDialog(
            images = images,
            onDismiss = { showTechDetailsDialog = false }
        )
    }
}

@Composable
private fun EmptyComparisonState(
    onPaste: () -> Unit,
    onPick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Privacy Badge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GreenBest.copy(alpha = 0.12f),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GreenBest,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "100% Private Sandbox • Zero Gallery Indexing",
                    color = GreenBest,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = "Compare Manga & Manhwa Quality",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Seamlessly compare scans from different sources side-by-side or using a split overlay slider without cluttering your phone gallery.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Workflow Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepItem(
                    step = "1",
                    title = "Open reader or browser",
                    desc = "Navigate to a page or chapter on Source A."
                )
                StepItem(
                    step = "2",
                    title = "Share image to MangaCompare",
                    desc = "Long-press image -> Share -> choose MangaCompare."
                )
                StepItem(
                    step = "3",
                    title = "Switch source & share again",
                    desc = "Open the same page on Source B -> Share again. It appends automatically!"
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPaste,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color(0xFF002228)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paste Image", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onPick,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pick from Device", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StepItem(step: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(CyanAccent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = CyanAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            )
        }
    }
}
