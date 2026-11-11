package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.clipboard.ClipboardImageHelper
import com.example.data.clipboard.ClipboardResult
import com.example.data.local.AppDatabase
import com.example.data.local.ComparisonSessionEntity
import com.example.data.metadata.MetadataExtractor
import com.example.data.model.ComparisonImage
import com.example.data.repository.ComparisonRepository
import com.example.data.storage.PrivateStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ComparisonMode {
    SIDE_BY_SIDE,
    OVERLAY_SLIDER
}

class ComparisonViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = PrivateStorageManager(application)
    private val database = AppDatabase.getInstance(application)
    private val repository = ComparisonRepository(database.comparisonDao(), storageManager)
    private val clipboardHelper = ClipboardImageHelper(application)

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    val currentImages: StateFlow<List<ComparisonImage>> = _currentSessionId
        .flatMapLatest { sessionId ->
            repository.getImagesForSession(sessionId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allSessions: StateFlow<List<ComparisonSessionEntity>> = repository.getAllSessions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _comparisonMode = MutableStateFlow(ComparisonMode.SIDE_BY_SIDE)
    val comparisonMode: StateFlow<ComparisonMode> = _comparisonMode.asStateFlow()

    // Slider active indices
    private val _sliderPair = MutableStateFlow(Pair(0, 1))
    val sliderPair: StateFlow<Pair<Int, Int>> = _sliderPair.asStateFlow()

    // Draggable split divider position (0.0 to 1.0)
    private val _sliderDividerPosition = MutableStateFlow(0.5f)
    val sliderDividerPosition: StateFlow<Float> = _sliderDividerPosition.asStateFlow()

    // Sync trigger for relative percentage scroll
    private val _alignScrollTrigger = MutableStateFlow<Float?>(null)
    val alignScrollTrigger: StateFlow<Float?> = _alignScrollTrigger.asStateFlow()

    // Notification toast / banner message
    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    // Total private storage used
    private val _storageUsedBytes = MutableStateFlow(0L)
    val storageUsedBytes: StateFlow<Long> = _storageUsedBytes.asStateFlow()

    // Auto-prune preference
    private val _autoPruneEnabled = MutableStateFlow(true)
    val autoPruneEnabled: StateFlow<Boolean> = _autoPruneEnabled.asStateFlow()

    init {
        refreshStorageUsage()
        // Run auto-prune on startup if enabled
        viewModelScope.launch {
            if (_autoPruneEnabled.value) {
                repository.autoPrune(7)
                refreshStorageUsage()
            }
        }
    }

    fun setComparisonMode(mode: ComparisonMode) {
        _comparisonMode.value = mode
    }

    fun setSliderDividerPosition(pos: Float) {
        _sliderDividerPosition.value = pos.coerceIn(0.05f, 0.95f)
    }

    fun setSliderPair(firstIndex: Int, secondIndex: Int) {
        _sliderPair.value = Pair(firstIndex, secondIndex)
    }

    fun swapSliderImages() {
        val current = _sliderPair.value
        _sliderPair.value = Pair(current.second, current.first)
    }

    /**
     * Appends imported image URIs to the active comparison session without clearing existing items.
     * Preserves existing images and their associated notes.
     */
    fun importUris(uris: List<Uri>, defaultNotePrefix: String = "") {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var addedCount = 0
            val existingCount = currentImages.value.size
            for ((index, uri) in uris.withIndex()) {
                try {
                    val note = if (defaultNotePrefix.isNotBlank()) {
                        "$defaultNotePrefix ${existingCount + index + 1}"
                    } else "Source ${existingCount + index + 1}"

                    repository.importImageUri(uri, _currentSessionId.value, note)
                    addedCount++
                } catch (e: Exception) {
                    _statusMessage.value = "Failed to import image: ${e.localizedMessage ?: "Unknown error"}"
                }
            }
            if (addedCount > 0) {
                _statusMessage.value = if (addedCount == 1) "Added 1 image to comparison" else "Added $addedCount images to comparison"
                refreshStorageUsage()
            }
        }
    }

    /**
     * Pastes image directly from Android Clipboard.
     */
    fun pasteFromClipboard() {
        when (val result = clipboardHelper.getClipboardImage()) {
            is ClipboardResult.SuccessUri -> {
                importUris(listOf(result.uri), defaultNotePrefix = "Pasted")
            }
            is ClipboardResult.SuccessBitmap -> {
                viewModelScope.launch {
                    try {
                        val count = currentImages.value.size
                        repository.importBitmap(
                            result.bitmap,
                            _currentSessionId.value,
                            defaultNote = "Clipboard ${count + 1}"
                        )
                        _statusMessage.value = "Pasted image from clipboard"
                        refreshStorageUsage()
                    } catch (e: Exception) {
                        _statusMessage.value = "Failed to save clipboard bitmap: ${e.localizedMessage}"
                    }
                }
            }
            is ClipboardResult.Error -> {
                _statusMessage.value = result.message
            }
        }
    }

    /**
     * Updates editable source note pinned to image's stable UUID.
     */
    fun updateNote(imageId: String, newNote: String) {
        viewModelScope.launch {
            repository.updateNote(imageId, newNote.trim())
        }
    }

    /**
     * Instantly removes a single image from the current comparison and cleans up its private file.
     * Triggered by the small 'x' button requested by the user.
     */
    fun removeSingleImage(image: ComparisonImage) {
        viewModelScope.launch {
            repository.removeSingleImage(image.id, image.localFilePath)
            _statusMessage.value = "Removed image"
            refreshStorageUsage()
        }
    }

    /**
     * Reorders an image forward or backward. Notes stay attached to their stable image IDs.
     */
    fun moveImage(fromIndex: Int, toIndex: Int) {
        val list = currentImages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            viewModelScope.launch {
                repository.reorderImages(list)
            }
        }
    }

    /**
     * Triggers relative percentage scroll synchronization across viewports.
     */
    fun triggerAlignScroll(percent: Float) {
        _alignScrollTrigger.value = percent
    }

    fun clearAlignScrollTrigger() {
        _alignScrollTrigger.value = null
    }

    /**
     * Starts a fresh comparison session without deleting history.
     */
    fun startNewSession() {
        _currentSessionId.value = UUID.randomUUID().toString()
        _statusMessage.value = "Started new comparison session"
    }

    /**
     * Loads a previous session from history.
     */
    fun loadSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _statusMessage.value = "Loaded session"
    }

    /**
     * Deletes a session and its files from history.
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                startNewSession()
            }
            _statusMessage.value = "Deleted session"
            refreshStorageUsage()
        }
    }

    /**
     * Clears all comparison images and history completely.
     */
    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            startNewSession()
            _statusMessage.value = "Cleared all comparisons and private storage"
            refreshStorageUsage()
        }
    }

    fun toggleAutoPrune(enabled: Boolean) {
        _autoPruneEnabled.value = enabled
        if (enabled) {
            viewModelScope.launch {
                repository.autoPrune(7)
                refreshStorageUsage()
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun refreshStorageUsage() {
        viewModelScope.launch {
            _storageUsedBytes.value = repository.getStorageUsedBytes()
        }
    }
}
