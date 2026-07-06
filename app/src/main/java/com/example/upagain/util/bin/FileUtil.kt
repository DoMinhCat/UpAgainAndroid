package com.example.upagain.util.bin

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat.getString
import com.example.upagain.R
import com.example.upagain.util.ui.SnackbarLevel
import com.example.upagain.util.ui.showTopSnackbar
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

fun saveBarcodeImage(context: Context, base64Str: String, transactionId: String, main: View) {
    try {
        val cleanBase64 = base64Str.substringAfter("base64,")
        val decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "barcode-$transactionId.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
            put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val imageUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
            contentValues.clear()
            contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
            main.showTopSnackbar(context.getString(R.string.barcode_saved_success), SnackbarLevel.SUCCESS)
        } else {
            main.showTopSnackbar(context.getString(R.string.barcode_saved_fail, ""), SnackbarLevel.ERROR)
        }
    } catch (e: Exception) {
        Log.e("ShopDetailFragment", "Error saving barcode", e)
        main.showTopSnackbar(context.getString(R.string.barcode_saved_fail, e.message ?: ""), SnackbarLevel.ERROR)
    }
}