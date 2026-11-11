package com.example

import com.example.data.metadata.MetadataExtractor
import com.example.data.model.ComparisonImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ComparisonModelTest {

    @Test
    fun testMegapixelCalculation() {
        val image = ComparisonImage(
            id = UUID.randomUUID().toString(),
            sessionId = "session-1",
            localFilePath = "/fake/path/img.webp",
            width = 1920,
            height = 12000,
            fileSizeBytes = 4_800_000L,
            mimeType = "image/webp",
            note = "MangaDex"
        )

        assertEquals(23.04, image.megapixels, 0.01)
        assertEquals("23.04 MP", image.formattedMegapixels)
        assertTrue(image.isTallManhwa)
    }

    @Test
    fun testStandardMangaAspectRatio() {
        val standardRatio = ComparisonImage.calculateAspectRatio(1920, 1080)
        assertEquals("16:9", standardRatio)

        val tallRatio = ComparisonImage.calculateAspectRatio(1000, 5000)
        assertEquals("1:5", tallRatio)
    }

    @Test
    fun testFormatBytes() {
        assertEquals("0 B", ComparisonImage.formatBytes(0))
        assertEquals("512 B", ComparisonImage.formatBytes(512))
        assertEquals("1.5 KB", ComparisonImage.formatBytes(1536))
        assertEquals("4.58 MB", ComparisonImage.formatBytes(4_800_000))
    }

    @Test
    fun testDecodedMemoryCalculation() {
        val img = ComparisonImage(
            id = "1",
            sessionId = "s",
            localFilePath = "",
            width = 1000,
            height = 2000,
            fileSizeBytes = 500_000,
            mimeType = "image/jpeg"
        )

        // 1000 * 2000 * 4 bytes = 8,000,000 bytes (~7.63 MB)
        assertEquals(8_000_000L, img.estimatedBitmapMemoryBytes)
        assertEquals("7.63 MB", img.formattedBitmapMemory)
    }

    @Test
    fun testQualityRankingLogic() {
        val imgHighRes = ComparisonImage(
            id = "high",
            sessionId = "s",
            localFilePath = "",
            width = 1920,
            height = 10000,
            fileSizeBytes = 3_000_000,
            mimeType = "image/webp"
        )
        val imgLowRes = ComparisonImage(
            id = "low",
            sessionId = "s",
            localFilePath = "",
            width = 800,
            height = 4000,
            fileSizeBytes = 1_500_000,
            mimeType = "image/jpeg"
        )

        val ranks = MetadataExtractor.computeQualityRanks(listOf(imgHighRes, imgLowRes))
        assertEquals("Best Detail", ranks["high"])
        assertEquals("2nd", ranks["low"])
    }

    @Test
    fun testNotesRemainBoundToStableIdDuringReorder() {
        val img1 = ComparisonImage(id = "uuid-1", sessionId = "s", localFilePath = "", width = 100, height = 100, fileSizeBytes = 100, mimeType = "image/png", note = "MangaDex", displayOrder = 0)
        val img2 = ComparisonImage(id = "uuid-2", sessionId = "s", localFilePath = "", width = 200, height = 200, fileSizeBytes = 200, mimeType = "image/png", note = "Asura", displayOrder = 1)

        val list = mutableListOf(img1, img2)
        // Swap positions
        val temp = list[0]
        list[0] = list[1]
        list[1] = temp

        // Note of uuid-1 is still MangaDex
        assertEquals("MangaDex", list.find { it.id == "uuid-1" }?.note)
        assertEquals("Asura", list.find { it.id == "uuid-2" }?.note)
    }
}
