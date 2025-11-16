package com.rejown.pixelbeam.domain.util

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/**
 * Utility class for compressing images to target size
 */
class ImageCompressor(private val context: Context) {

    companion object {
        private const val TARGET_SIZE_KB = 100
        private const val MIN_QUALITY = 50
        private const val INITIAL_QUALITY = 90
    }

    /**
     * Compress image from URI to target size (~100KB)
     * @return Compressed bitmap or null if compression fails
     */
    suspend fun compressImage(uri: Uri): CompressedImage? = withContext(Dispatchers.IO) {
        try {
            // Get actual file size from URI
            val originalFileSize = getFileSize(uri)

            // Load original bitmap
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return@withContext null

            // Compress to target size
            val compressedBitmap = resizeToTargetSize(originalBitmap, TARGET_SIZE_KB)
            val compressedBytes = bitmapToBytes(compressedBitmap)

            CompressedImage(
                originalBitmap = originalBitmap,
                compressedBitmap = compressedBitmap,
                originalSizeBytes = originalFileSize,
                compressedSizeBytes = compressedBytes.size.toLong(),
                compressedBytes = compressedBytes
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Resize bitmap to target size using iterative approach
     */
    private suspend fun resizeToTargetSize(
        bitmap: Bitmap,
        targetSizeKB: Int
    ): Bitmap = withContext(Dispatchers.Default) {
        var quality = INITIAL_QUALITY
        var resizedBitmap = bitmap
        var byteArray: ByteArray

        do {
            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(
                Bitmap.CompressFormat.JPEG,
                quality,
                outputStream
            )
            byteArray = outputStream.toByteArray()

            if (byteArray.size > targetSizeKB * 1024) {
                // Calculate scale factor
                val scale = sqrt((targetSizeKB * 1024.0) / byteArray.size)
                val newWidth = (resizedBitmap.width * scale).toInt()
                val newHeight = (resizedBitmap.height * scale).toInt()

                // Create scaled bitmap
                resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    newWidth,
                    newHeight,
                    true
                )
                quality = maxOf(MIN_QUALITY, quality - 5)
            } else {
                break
            }
        } while (byteArray.size > targetSizeKB * 1024 && quality >= MIN_QUALITY)

        resizedBitmap
    }

    /**
     * Convert bitmap to byte array
     */
    private fun bitmapToBytes(bitmap: Bitmap, quality: Int = 90): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Get actual file size from URI in bytes using ContentResolver
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            var fileSize = 0L

            // Try to get file size from content resolver using cursor
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }

            // Fallback: try to read the actual file size
            if (fileSize == 0L) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    var totalBytes = 0L
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalBytes += bytesRead
                    }
                    fileSize = totalBytes
                }
            }

            fileSize
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}

/**
 * Data class representing compressed image result
 */
data class CompressedImage(
    val originalBitmap: Bitmap,
    val compressedBitmap: Bitmap,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val compressedBytes: ByteArray
) {
    val originalSizeKB: Double get() = originalSizeBytes / 1024.0
    val compressedSizeKB: Double get() = compressedSizeBytes / 1024.0
    val compressionRatio: Double get() = (compressedSizeBytes.toDouble() / originalSizeBytes) * 100

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
