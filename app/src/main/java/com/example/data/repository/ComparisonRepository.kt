package com.example.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.ComparisonDao
import com.example.data.local.ComparisonImageEntity
import com.example.data.local.ComparisonSessionEntity
import com.example.data.model.ComparisonImage
import com.example.data.storage.PrivateStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ComparisonRepository(
    private val dao: ComparisonDao,
    private val storageManager: PrivateStorageManager
) {

    fun getAllSessions(): Flow<List<ComparisonSessionEntity>> = dao.getAllSessions()

    fun getImagesForSession(sessionId: String): Flow<List<ComparisonImage>> {
        return dao.getImagesForSession(sessionId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    suspend fun getOrCreateSession(sessionId: String, title: String = "Comparison"): ComparisonSessionEntity = withContext(Dispatchers.IO) {
        val existing = dao.getSessionById(sessionId)
        if (existing != null) {
            existing
        } else {
            val newSession = ComparisonSessionEntity(
                sessionId = sessionId,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            dao.insertSession(newSession)
            newSession
        }
    }

    suspend fun importImageUri(uri: Uri, sessionId: String, defaultNote: String = ""): ComparisonImage = withContext(Dispatchers.IO) {
        // Ensure session exists
        getOrCreateSession(sessionId)

        // Save image to private storage (.nomedia guaranteed)
        val image = storageManager.saveImageFromUri(uri, sessionId, defaultNote)

        // Assign display order at end of list
        val currentImages = dao.getImagesForSessionSync(sessionId)
        val orderedImage = image.copy(displayOrder = currentImages.size)

        // Insert into Room
        dao.insertImage(ComparisonImageEntity.fromDomainModel(orderedImage))

        // Update session timestamp
        dao.insertSession(ComparisonSessionEntity(
            sessionId = sessionId,
            title = "Comparison (${currentImages.size + 1} images)",
            updatedAt = System.currentTimeMillis()
        ))

        orderedImage
    }

    suspend fun importBitmap(bitmap: Bitmap, sessionId: String, defaultNote: String = "Clipboard"): ComparisonImage = withContext(Dispatchers.IO) {
        getOrCreateSession(sessionId)
        val image = storageManager.saveBitmap(bitmap, sessionId, defaultNote)
        val currentImages = dao.getImagesForSessionSync(sessionId)
        val orderedImage = image.copy(displayOrder = currentImages.size)

        dao.insertImage(ComparisonImageEntity.fromDomainModel(orderedImage))
        dao.insertSession(ComparisonSessionEntity(
            sessionId = sessionId,
            title = "Comparison (${currentImages.size + 1} images)",
            updatedAt = System.currentTimeMillis()
        ))

        orderedImage
    }

    suspend fun updateNote(imageId: String, note: String) = withContext(Dispatchers.IO) {
        dao.updateImageNote(imageId, note)
    }

    suspend fun reorderImages(images: List<ComparisonImage>) = withContext(Dispatchers.IO) {
        images.forEachIndexed { index, img ->
            dao.updateImageOrder(img.id, index)
        }
    }

    /**
     * Removes a single image from the session and deletes its private storage file.
     * Triggered directly by the user's requested 'x' button.
     */
    suspend fun removeSingleImage(imageId: String, filePath: String) = withContext(Dispatchers.IO) {
        dao.deleteImageById(imageId)
        storageManager.deleteImageFile(filePath)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        val filePaths = dao.getFilePathsForSession(sessionId)
        dao.deleteSessionById(sessionId)
        storageManager.deleteFiles(filePaths)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.deleteAllImages()
        dao.deleteAllSessions()
        storageManager.clearAllPrivateFiles()
    }

    suspend fun getStorageUsedBytes(): Long = storageManager.calculateTotalStorageUsedBytes()

    suspend fun autoPrune(retentionDays: Int = 7) = withContext(Dispatchers.IO) {
        storageManager.autoPruneOldFiles(retentionDays)
    }
}
