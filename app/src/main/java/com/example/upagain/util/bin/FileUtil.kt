package com.example.upagain.util.bin

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

/**
 * 1. Resolves the MIME type and matching file extension from a Content URI.
 */
fun getFileExtensionAndMime(context: Context, uri: Uri): Pair<String, String> {
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
    return Pair(mimeType, extension)
}

/**
 * 2. Streams data from the Content URI into a temporary file in the app cache cacheDir.
 */
fun streamUriToTempFile(context: Context, uri: Uri, tempFile: File): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        true
    } catch (e: Exception) {
        false
    }
}