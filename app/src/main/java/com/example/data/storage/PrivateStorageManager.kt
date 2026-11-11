package com.example.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.metadata.ExtractedMetadata
import com.example.data.metadata.MetadataExtractor
import com.example.data.model.ComparisonImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class PrivateStorageManager(private val context: Context) {

    private val comparisonsDir: File
        get() {
            // Strictly private internal application storage: /data/user/0/<package>/files/comparisons
            val dir = File(context.filesDir, "comparisons")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            // Sentinel .nomedia file ensures gallery indexers and media scanners never scan this folder
            val noMedia = File(dir, ".nomedia")
            if (!noMedia.exists()) {
                try {
                    noMedia.createNewFile()
                } catch (ignored: Exception) {}
            }
            return dir
        }

    /**
     * Copies image data from a content:// or file:// URI directly into private internal storage.
     * Never writes to MediaStore or public DCIM/Pictures.
     */
    suspend fun saveImageFromUri(uri: Uri, sessionId: String, initialNote: String = ""): ComparisonImage = withContext(Dispatchers.IO) {
        val imageId = UUID.randomUUID().toString()
        val tempDest = File(comparisonsDir, "${imageId}.tmp")

        var originalMime = context.contentResolver.getType(uri) ?: "image/jpeg"

        // Copy input stream to private storage file
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempDest).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Cannot open stream for URI: $uri")

        // Inspect header without full bitmap allocation
        val metadata = MetadataExtractor.extractFromFile(tempDest)
        if (metadata.mimeType != "image/unknown") {
            originalMime = metadata.mimeType
        }

        // Determine extension
        val ext = when {
            originalMime.contains("webp", ignoreCase = true) -> "webp"
            originalMime.contains("png", ignoreCase = true) -> "png"
            originalMime.contains("gif", ignoreCase = true) -> "gif"
            originalMime.contains("avif", ignoreCase = true) -> "avif"
            else -> "jpg"
        }

        val finalFile = File(comparisonsDir, "${imageId}.$ext")
        tempDest.renameTo(finalFile)

        ComparisonImage(
            id = imageId,
            sessionId = sessionId,
            localFilePath = finalFile.absolutePath,
            width = metadata.width,
            height = metadata.height,
            fileSizeBytes = finalFile.length(),
            mimeType = originalMime,
            note = initialNote,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Saves a direct Bitmap (e.g. from clipboard ClipData) into private internal storage.
     */
    suspend fun saveBitmap(bitmap: Bitmap, sessionId: String, initialNote: String = "Clipboard"): ComparisonImage = withContext(Dispatchers.IO) {
        val imageId = UUID.randomUUID().toString()
        val finalFile = File(comparisonsDir, "${imageId}.png")

        FileOutputStream(finalFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        ComparisonImage(
            id = imageId,
            sessionId = sessionId,
            localFilePath = finalFile.absolutePath,
            width = bitmap.width,
            height = bitmap.height,
            fileSizeBytes = finalFile.length(),
            mimeType = "image/png",
            note = initialNote,
            createdAt = System.currentTimeMillis()
        )
    }

    /**
     * Deletes a single image file immediately when the user taps the 'x' button.
     */
    suspend fun deleteImageFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists() && file.parentFile?.absolutePath == comparisonsDir.absolutePath) {
                file.delete()
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Deletes all files associated with a list of file paths.
     */
    suspend fun deleteFiles(filePaths: List<String>) = withContext(Dispatchers.IO) {
        filePaths.forEach { path ->
            deleteImageFile(path)
        }
    }

    /**
     * Clears all private comparison files.
     */
    suspend fun clearAllPrivateFiles() = withContext(Dispatchers.IO) {
        comparisonsDir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia") {
                file.delete()
            }
        }
    }

    /**
     * Automatically prunes files older than retentionDays.
     */
    suspend fun autoPruneOldFiles(retentionDays: Int = 7) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (retentionDays * 24L * 60L * 60L * 1000L)
        comparisonsDir.listFiles()?.forEach { file ->
            if (file.name != ".nomedia" && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    /**
     * Returns total bytes used by private comparison files.
     */
    suspend fun calculateTotalStorageUsedBytes(): Long = withContext(Dispatchers.IO) {
        comparisonsDir.listFiles()?.filter { it.name != ".nomedia" }?.sumOf { it.length() } ?: 0L
    }
}
