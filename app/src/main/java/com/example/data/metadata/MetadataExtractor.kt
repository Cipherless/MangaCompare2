package com.example.data.metadata

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

data class ExtractedMetadata(
    val width: Int,
    val height: Int,
    val mimeType: String,
    val fileSizeBytes: Long
)

object MetadataExtractor {

    /**
     * Inspects image headers without decoding the full bitmap into memory.
     * Prevents OutOfMemoryError on ultra-tall manhwa images.
     */
    fun extractFromFile(file: File): ExtractedMetadata {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        FileInputStream(file).use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val detectedMime = options.outMimeType ?: detectMimeTypeFromMagicBytes(file)
        val fileSizeBytes = file.length()

        return ExtractedMetadata(
            width = options.outWidth.coerceAtLeast(0),
            height = options.outHeight.coerceAtLeast(0),
            mimeType = detectedMime,
            fileSizeBytes = fileSizeBytes
        )
    }

    /**
     * Inspects magic bytes from the file header to accurately detect format.
     */
    fun detectMimeTypeFromMagicBytes(file: File): String {
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(12)
                val read = input.read(header)
                if (read < 4) return "image/unknown"

                when {
                    // JPEG: FF D8 FF
                    header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte() && header[2] == 0xFF.toByte() -> "image/jpeg"
                    // PNG: 89 50 4E 47 0D 0A 1A 0A
                    header[0] == 0x89.toByte() && header[1] == 0x50.toByte() && header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() -> "image/png"
                    // GIF: 47 49 46 38
                    header[0] == 0x47.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x38.toByte() -> "image/gif"
                    // WebP: 52 49 46 46 ... 57 45 42 50 ("RIFF" ... "WEBP")
                    read >= 12 && header[0] == 0x52.toByte() && header[1] == 0x49.toByte() && header[2] == 0x46.toByte() && header[3] == 0x46.toByte() &&
                            header[8] == 0x57.toByte() && header[9] == 0x45.toByte() && header[10] == 0x42.toByte() && header[11] == 0x50.toByte() -> "image/webp"
                    // AVIF / HEIF: ....ftypavif or ftyp
                    read >= 12 && header[4] == 0x66.toByte() && header[5] == 0x74.toByte() && header[6] == 0x79.toByte() && header[7] == 0x70.toByte() -> "image/avif"
                    else -> "image/unknown"
                }
            }
        } catch (e: Exception) {
            "image/unknown"
        }
    }

    /**
     * Computes relative quality rankings among a list of images.
     * Evaluates effective megapixels, resolution width, and compression efficiency.
     * Higher resolution combined with balanced byte density is favored.
     */
    fun computeQualityRanks(images: List<com.example.data.model.ComparisonImage>): Map<String, String> {
        if (images.size < 2) return emptyMap()

        // Calculate score for each image:
        // Score = (Megapixels * 100) + (width / 10.0) + (bytesPerPixel weighting)
        val scoredList = images.map { img ->
            val totalPixels = (img.width.toDouble() * img.height.toDouble()).coerceAtLeast(1.0)
            val bytesPerPixel = if (totalPixels > 0) img.fileSizeBytes.toDouble() / totalPixels else 0.0

            // Modern formats (WebP, AVIF) achieve equal visual clarity at lower byte sizes
            val formatMultiplier = when {
                img.mimeType.contains("avif", ignoreCase = true) -> 1.3
                img.mimeType.contains("webp", ignoreCase = true) -> 1.2
                img.mimeType.contains("png", ignoreCase = true) -> 1.05
                else -> 1.0
            }

            val score = (img.megapixels * 50.0) + (img.width.toDouble() * 0.1) + (bytesPerPixel * 100.0 * formatMultiplier)
            img.id to score
        }.sortedByDescending { it.second }

        val ranks = mutableMapOf<String, String>()
        scoredList.forEachIndexed { index, pair ->
            val label = when (index) {
                0 -> "Best Detail"
                1 -> "2nd"
                2 -> "3rd"
                else -> "${index + 1}th"
            }
            ranks[pair.first] = label
        }
        return ranks
    }
}
