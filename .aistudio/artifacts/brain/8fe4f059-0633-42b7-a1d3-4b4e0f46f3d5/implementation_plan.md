# MangaCompare — Native Android Manga & Manhwa Comparison App

A privacy-focused, high-performance native Android application engineered specifically for comparing manga and manhwa image quality across different scanlation sources. Operates 100% locally with private internal storage, zero gallery contamination, memory-efficient rendering for ultra-tall manhwa strips, per-image source notes, and dual comparison modes (Side-by-Side and Interactive Overlay Slider).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions have been confirmed based on your requirements and preferences:

- **Single-Image Removal Button ('x')**: **Prominent, compact 'x' close button on each image's header card**. Tapping it immediately drops that image from the active comparison session, cleans up its associated internal storage file, and adjusts the layout seamlessly without disturbing the remaining images or their notes.
- **Long Manhwa Scrolling Synchronization**: **Independent scrolling by default with a quick "Align %" button**. This lets you freely pan either image to any section, then tap the alignment button to immediately lock both images to the exact same relative vertical percentage (e.g. 45% through the chapter).
- **Private Storage & Retention**: **Keep sessions until manually cleared, with an optional 7-day auto-prune setting** in the app drawer. Images are written exclusively to `context.filesDir/comparisons/` accompanied by a `.nomedia` file to guarantee zero gallery indexing.
- **Quality Indicators**: **Enabled with subtle, unobtrusive chips** (e.g. `Best Detail`, `2nd`) evaluated from resolution, compression density, and format, without reordering your chosen images or cluttering the view.
- **GitHub Actions & APK Delivery**: A preconfigured `.github/workflows/build-apk.yml` workflow will be created so you can build and download ready-to-install debug/release APKs directly from GitHub on your phone without needing a developer PC.

---

## 1. Overview & Core Concept

### What It Does
MangaCompare allows readers using Reikai (or any Android app or browser) to inspect image quality variations across sources (e.g., MangaDex vs. Asura Scans vs. Reaper Scans). Users can share an image directly into MangaCompare, switch sources, share another image into the same session, and inspect both side-by-side or using a split overlay slider.

### Primary User Workflow
```
[Reikai / Source A] ──(Share Image)──┐
                                     ▼
                              [MangaCompare] ◄── Appends to current session
                                     ▲           (Stores in private app storage)
[Reikai / Source B] ──(Share Image)──┘
        │
        ▼ (or tap "Paste" from clipboard or "Pick Image" from system picker)
[Dual Mode View] ──► Side-by-Side OR Interactive Overlay Slider
[Inspection]     ──► Technical metadata (WxH, file size, MP, format, ratio)
[Per-Image Note] ──► Tag source name ("MangaDex", "Asura") bound to image UUID
[Single 'x' Del] ──► Instantly remove unwanted images from current view
[Session Save]   ──► Local Room DB history (Zero gallery pollution)
```

### Key Values
- **Zero Gallery Pollution**: Never writes to `DCIM`, `Pictures`, or `MediaStore`. Android's media scanner is strictly blocked.
- **Append on Share**: Sharing subsequent images adds to the ongoing comparison rather than overwriting existing items.
- **Quick Single-Image Removal**: Dedicated 'x' button on every image card for instantaneous pruning.
- **Subsampled / Tiled Rendering for Manhwa**: Prevents `OutOfMemoryError` on 20,000px+ tall manhwa webtoons via memory-conscious decoding.
- **Per-Image Source Notes**: Notes attach to stable image IDs and follow images across reordering, slider switching, and history loads.

---

## 2. User Experience & Visual Design

### Aesthetic Direction & Visual Theme
- **Theme**: Luxury Utilitarian Dark Mode (`#0D0F12` deep slate black background, `#171A21` elevated surface cards, `#00E5FF` cyan accent for active slider/controls, `#7C4DFF` deep purple secondary accent).
- **Typography**: Clean monospace / tabular numbers for technical specs (resolution, megapixels, file sizes) with readable sans-serif for labels and notes.
- **Layout**: Full-screen immersive canvas maximizing image area; compact floating toolbars and collapsible bottom sheet for technical metadata and source notes.

### Main Screens & Modes

1. **Comparison Studio (Main Screen)**:
   - **Top App Bar**: Session Title, Comparison Mode Switcher (Side-by-Side vs. Overlay Slider), "Align %" sync button, and Overflow Menu (Settings, History, Clear All).
   - **Viewport Area**:
     - *Side-by-Side Mode*: Two (or more) viewports with synchronized zoom and independent scroll + relative % alignment toggle.
     - *Overlay Slider Mode*: Bottom image layered beneath top image with a smooth, draggable vertical divider bar and handle. A 1-tap "Swap Top/Bottom" toggle and pair selector.
   - **Floating Action Dock**:
     - `Paste` button (reads image directly from Android `ClipboardManager` / `ClipData`).
     - `Add Image` button (system Photo Picker).
     - `Notes & Specs` toggle button.
   - **Per-Image Info & Note Bar**: Compact header pinned above or over each image displaying:
     - **'x' Close Button**: Small circular button (`Icons.Default.Close`) positioned at the top right of each image card to immediately remove that single image.
     - **Editable Source Tag**: (e.g., `MangaDex` with quick-edit inline dialog).
     - **Quality Chip**: (e.g., `Best Detail`, `2nd`).
     - **Tech Specs**: `1920×12000` • `4.8 MB` • `WebP` • `23.04 MP` • `4:25`.
     - **Ordering Controls**: Move Left / Move Right.

2. **History Screen**:
   - List of saved comparison sessions with thumbnail previews, image counts, timestamps, and source notes summary.
   - Actions: Restore Session, Delete Session, Clear All History.

3. **Storage & Settings Sheet**:
   - Storage usage indicator (MB used by private comparison files).
   - "Clear App Cache & Temp Files" button.
   - Auto-prune toggle (Retain 7 days / Keep indefinitely).

---

## 3. Technical Architecture & Data Strategy

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANDROID SYSTEM LAYER                            │
│  ACTION_SEND / ACTION_SEND_MULTIPLE │ ClipboardManager │ Photo Picker  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ (content:// URI or ClipData)
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          INGESTION ENGINE                              │
│  - Stream Copier -> App Private Storage (context.filesDir/comparisons) │
│  - .nomedia sentinel validation                                        │
│  - BitmapFactory.Options.inJustDecodeBounds (Header-only parsing)      │
│  - Original stream length measurement (exact file size)                │
│  - MIME type / Magic Bytes inspection (JPEG, PNG, WebP, AVIF)          │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ ImageMetadata + Local File
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     REPOSITORY & LOCAL STORAGE                         │
│  - Room Database (AppDatabase): ComparisonSessionEntity, ImageItemEntity│
│  - Stable UUID associations (Note, Order, Metrics, Path)               │
│  - Single Image Deletion ('x' action) & Session Purge Handlers         │
│  - Auto-Prune & Manual Cleaner Services                                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ StateFlow<SessionUiState>
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                         JETPACK COMPOSE UI                             │
│  - ComparisonViewModel (Session state, mode toggling, note editing)    │
│  - ManhwaTiledViewer / LargeImageCanvas (Memory-safe tiled rendering)  │
│  - SplitOverlaySlider (Draggable touch divider, before/after mask)     │
│  - SideBySideComparator (Independent scroll + relative % sync button)  │
│  - ImageHeaderCard with 'x' button & InlineNoteEditor                  │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Entity Strategy

```kotlin
// Stable image item in private app storage
data class ComparisonImage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val localFilePath: String,      // strictly inside context.filesDir/comparisons/
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val mimeType: String,
    val note: String = "",          // Bound to image UUID, survives reordering
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val megapixels: Double get() = (width.toLong() * height) / 1_000_000.0
    val aspectRatioString: String get() = calculateAspectRatio(width, height)
    val formattedFileSize: String get() = formatBytes(fileSizeBytes)
}
```

### Memory-Safe Manhwa Engine
1. **Header-Only Pre-Flight**: Never decode whole bitmaps during import. Use `inJustDecodeBounds = true` to capture width, height, and mime type in < 5ms without allocating bitmap memory.
2. **Subsampling & Downsampling**:
   - For images exceeding standard texture sizes (GL limit 4096px or 8192px), use Android's `BitmapRegionDecoder` to slice visible viewports or load downsampled mipmaps (`inSampleSize = 2, 4, 8`) dynamically during pan/zoom.
3. **No Gallery Leak Guarantee**:
   - Save path: `File(context.filesDir, "comparisons")`.
   - On initialization, verify `File(context.filesDir, "comparisons/.nomedia").createNewFile()`.
   - Never register files with `MediaScannerConnection` or write to `MediaStore`.

---

## 4. Implementation Steps & Verification Plan

### Phase 1: Build & Dependency Foundation
1. Update `app/build.gradle.kts` to enable `libs.coil.compose`, `libs.androidx.room`, and ensure modern compose libraries are configured.
2. Set application name in `res/values/strings.xml` and `metadata.json` to "MangaCompare".
3. Configure `AndroidManifest.xml` with:
   - `intent-filter` for `android.intent.action.SEND` and `android.intent.action.SEND_MULTIPLE` with MIME type `image/*`.
   - Single-task launch mode (`android:launchMode="singleTask"`) so sharing another image while the app is running triggers `onNewIntent` to append to the active comparison session.

### Phase 2: Ingestion & Storage Engine
1. `PrivateStorageManager`: Handles copying content URIs, clipboard bitmaps/URIs, and file picker streams directly into private internal files with `.nomedia`. Provides `deleteImageFile(filePath)` to immediately free storage when the 'x' button is pressed.
2. `MetadataExtractor`: Efficiently inspects stream headers without full bitmap allocation. Computes WxH, file size, format, megapixels, and aspect ratio.
3. `ClipboardImageHelper`: Interacts with `ClipboardManager` to retrieve `ClipData` image URIs and direct bitmaps.

### Phase 3: Room Database & Repository
1. Room entities: `SessionEntity` and `ImageItemEntity`.
2. DAOs for reactive Flow querying, inserting, updating notes by image ID, deleting items by image ID (for the 'x' button), and deleting whole sessions.
3. Auto-cleanup routine for purging private files older than retention policy.

### Phase 4: Jetpack Compose Comparison UI
1. `ImageHeaderCard`: Compact overlay header on every image containing:
   - Prominent, clean 'x' button (`Modifier.testTag("remove_image_<id>")`) to drop the single image.
   - Note editor chip with source name.
   - Quality badge and resolution/size chips.
2. `SideBySideView`: Dual column / row viewport with synchronized zoom and independent scroll + relative % alignment button.
3. `OverlaySliderView`: Interactive split divider with draggable touch handle, top/bottom layer switching, and opacity controls.
4. `HistoryDialog / Screen`: Browse and restore previous comparisons.

### Phase 5: Verification & Automated GitHub Actions
1. Compile and verify with `compile_applet`.
2. Verify Robolectric unit tests for metadata parsing, aspect ratio math, single-item removal, and Room database operations.
3. Create `.github/workflows/build-apk.yml` with automated APK artifact upload so the user can push to GitHub and directly download the compiled APK on their CMF Phone 1.
