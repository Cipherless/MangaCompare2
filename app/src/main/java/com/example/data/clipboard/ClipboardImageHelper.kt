package com.example.data.clipboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build

sealed class ClipboardResult {
    data class SuccessUri(val uri: Uri) : ClipboardResult()
    data class SuccessBitmap(val bitmap: Bitmap) : ClipboardResult()
    data class Error(val message: String) : ClipboardResult()
}

class ClipboardImageHelper(private val context: Context) {

    private val clipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun getClipboardImage(): ClipboardResult {
        if (!clipboardManager.hasPrimaryClip()) {
            return ClipboardResult.Error("Clipboard is empty. Copy an image in Reikai first.")
        }

        val clip = clipboardManager.primaryClip ?: return ClipboardResult.Error("No content on clipboard.")
        if (clip.itemCount == 0) {
            return ClipboardResult.Error("Clipboard contains no items.")
        }

        val description = clip.description
        val item = clip.getItemAt(0)

        // 1. Check for content:// URI in item
        val uri = item.uri
        if (uri != null) {
            val type = context.contentResolver.getType(uri)
            val isImageMime = description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST) ||
                    (type != null && type.startsWith("image/")) ||
                    description.hasMimeType("image/*")

            if (isImageMime || type != null) {
                return ClipboardResult.SuccessUri(uri)
            }
        }

        // 2. Check for text that might be a content:// or file:// URI string
        val text = item.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && (text.startsWith("content://") || text.startsWith("file://"))) {
            try {
                val parsedUri = Uri.parse(text)
                return ClipboardResult.SuccessUri(parsedUri)
            } catch (ignored: Exception) {}
        }

        // 3. Check for direct Bitmap in intent extras if present
        val intent = item.intent
        if (intent != null) {
            val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM)
            }
            if (streamUri != null) {
                return ClipboardResult.SuccessUri(streamUri)
            }
        }

        return ClipboardResult.Error("Clipboard does not contain an accessible image. Use 'Share' from Reikai or copy an image directly.")
    }
}
