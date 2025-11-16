package com.rejown.pixelbeam.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Utility class for reading original file data without compression
 */
class ImageCompressor(private val context: Context) {

    /**
     * Read original image file from URI without any compression
     * @return Original image data or null if reading fails
     */
    suspend fun compressImage(uri: Uri): CompressedImage? = withContext(Dispatchers.IO) {
        try {
            // Get filename from URI
            val filename = getFileName(uri) ?: "image_${System.currentTimeMillis()}.jpg"

            // Read original file bytes
            val originalBytes = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: return@withContext null

            // Calculate SHA-256 checksum of original file
            val fileChecksum = calculateSHA256(originalBytes)

            // Load bitmap for preview only (not sent)
            val previewBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return@withContext null

            CompressedImage(
                originalBitmap = previewBitmap,
                compressedBitmap = previewBitmap,
                originalSizeBytes = originalBytes.size.toLong(),
                compressedSizeBytes = originalBytes.size.toLong(),
                compressedBytes = originalBytes,
                filename = filename,
                fileChecksum = fileChecksum
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Calculate SHA-256 checksum of file bytes
     */
    private fun calculateSHA256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get filename from URI
     */
    private fun getFileName(uri: Uri): String? {
        return try {
            var fileName: String? = null
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Data class representing original image data (no compression)
 */
data class CompressedImage(
    val originalBitmap: Bitmap,
    val compressedBitmap: Bitmap,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val compressedBytes: ByteArray,
    val filename: String,
    val fileChecksum: String
) {
    val originalSizeKB: Double get() = originalSizeBytes / 1024.0
    val compressedSizeKB: Double get() = compressedSizeBytes / 1024.0
    val compressionRatio: Double get() = 100.0 // Always 100% since no compression

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressedImage

        if (!compressedBytes.contentEquals(other.compressedBytes)) return false

        return true
    }

    override fun hashCode(): Int {
        return compressedBytes.contentHashCode()
    }
}
