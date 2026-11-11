package com.example.data.model

import java.util.Locale
import java.util.UUID

data class ComparisonImage(
    val id: String = UUID.randomUUID().toString(),
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
    val megapixels: Double
        get() = if (width > 0 && height > 0) {
            (width.toDouble() * height.toDouble()) / 1_000_000.0
        } else 0.0

    val formattedMegapixels: String
        get() = String.format(Locale.US, "%.2f MP", megapixels)

    val aspectRatioString: String
        get() = calculateAspectRatio(width, height)

    val formattedFileSize: String
        get() = formatBytes(fileSizeBytes)

    val estimatedBitmapMemoryBytes: Long
        get() = width.toLong() * height.toLong() * 4L // ARGB_8888 4 bytes per pixel

    val formattedBitmapMemory: String
        get() = formatBytes(estimatedBitmapMemoryBytes)

    val formatDisplayName: String
        get() = when {
            mimeType.contains("webp", ignoreCase = true) -> "WebP"
            mimeType.contains("png", ignoreCase = true) -> "PNG"
            mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "JPEG"
            mimeType.contains("avif", ignoreCase = true) -> "AVIF"
            mimeType.contains("gif", ignoreCase = true) -> "GIF"
            else -> mimeType.substringAfterLast("/").uppercase(Locale.US).ifEmpty { "IMAGE" }
        }

    val isTallManhwa: Boolean
        get() = height > 0 && width > 0 && (height.toFloat() / width.toFloat()) >= 2.2f

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }

        fun calculateAspectRatio(width: Int, height: Int): String {
            if (width <= 0 || height <= 0) return "N/A"
            fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
            val g = gcd(width, height)
            val simplifiedW = width / g
            val simplifiedH = height / g

            return if (simplifiedW <= 32 && simplifiedH <= 32) {
                "$simplifiedW:$simplifiedH"
            } else {
                val ratio = height.toDouble() / width.toDouble()
                String.format(Locale.US, "1:%.2f", ratio)
            }
        }
    }
}
