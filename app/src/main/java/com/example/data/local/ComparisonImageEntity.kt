package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.ComparisonImage

@Entity(
    tableName = "comparison_images",
    foreignKeys = [
        ForeignKey(
            entity = ComparisonSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ComparisonImageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val localFilePath: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val mimeType: String,
    val note: String = "",
    val displayOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): ComparisonImage = ComparisonImage(
        id = id,
        sessionId = sessionId,
        localFilePath = localFilePath,
        width = width,
        height = height,
        fileSizeBytes = fileSizeBytes,
        mimeType = mimeType,
        note = note,
        displayOrder = displayOrder,
        createdAt = createdAt
    )

    companion object {
        fun fromDomainModel(model: ComparisonImage): ComparisonImageEntity = ComparisonImageEntity(
            id = model.id,
            sessionId = model.sessionId,
            localFilePath = model.localFilePath,
            width = model.width,
            height = model.height,
            fileSizeBytes = model.fileSizeBytes,
            mimeType = model.mimeType,
            note = model.note,
            displayOrder = model.displayOrder,
            createdAt = model.createdAt
        )
    }
}
