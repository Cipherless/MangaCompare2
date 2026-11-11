package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ComparisonDao {

    @Query("SELECT * FROM comparison_sessions ORDER BY updatedAt DESC")
    fun getAllSessions(): Flow<List<ComparisonSessionEntity>>

    @Query("SELECT * FROM comparison_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ComparisonSessionEntity?

    @Query("SELECT * FROM comparison_images WHERE sessionId = :sessionId ORDER BY displayOrder ASC, createdAt ASC")
    fun getImagesForSession(sessionId: String): Flow<List<ComparisonImageEntity>>

    @Query("SELECT * FROM comparison_images WHERE sessionId = :sessionId ORDER BY displayOrder ASC, createdAt ASC")
    suspend fun getImagesForSessionSync(sessionId: String): List<ComparisonImageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ComparisonSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ComparisonImageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<ComparisonImageEntity>)

    @Query("UPDATE comparison_images SET note = :note WHERE id = :imageId")
    suspend fun updateImageNote(imageId: String, note: String)

    @Query("UPDATE comparison_images SET displayOrder = :order WHERE id = :imageId")
    suspend fun updateImageOrder(imageId: String, order: Int)

    @Query("DELETE FROM comparison_images WHERE id = :imageId")
    suspend fun deleteImageById(imageId: String)

    @Query("DELETE FROM comparison_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM comparison_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM comparison_images")
    suspend fun deleteAllImages()

    @Query("SELECT localFilePath FROM comparison_images WHERE sessionId = :sessionId")
    suspend fun getFilePathsForSession(sessionId: String): List<String>

    @Query("SELECT localFilePath FROM comparison_images")
    suspend fun getAllFilePaths(): List<String>
}
