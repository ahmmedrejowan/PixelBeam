package com.rejown.pixelbeam.domain.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.rejown.pixelbeam.data.model.QRChunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.security.MessageDigest

/**
 * Utility class for reconstructing images from scanned QR code chunks.
 *
 * This class handles:
 * - Validating chunk data with MD5 checksums
 * - Combining Base64-encoded chunks into complete image data
 * - Decoding the combined data into a Bitmap
 * - Error handling for corrupted or missing data
 */
class ImageReconstructor {

    companion object {
        private const val TAG = "ImageReconstructor"
    }

    /**
     * Reconstructs original file data from a list of QR code chunks.
     *
     * @param chunks List of scanned QR chunks, must be sorted by index
     * @return Original file bytes, or null if reconstruction fails
     */
    suspend fun reconstructImageBytes(chunks: List<QRChunk>): ByteArray? = withContext(Dispatchers.Default) {
        try {
            Log.d(TAG, "Reconstructing file from ${chunks.size} chunks")

            // Filter out metadata chunks and get data chunks only
            val dataChunks = chunks.filter { !it.isMetadata && it.data != null }

            if (dataChunks.isEmpty()) {
                Log.e(TAG, "No data chunks found")
                return@withContext null
            }

            // Validate checksums
            for (chunk in dataChunks) {
                if (!validateChecksum(chunk)) {
                    Log.e(TAG, "Checksum validation failed for chunk ${chunk.index}")
                    return@withContext null
                }
            }

            // Combine all chunk data
            val combinedData = StringBuilder()
            dataChunks.forEach { chunk ->
                combinedData.append(chunk.data)
            }

            Log.d(TAG, "Combined data length: ${combinedData.length} chars")

            // Decode Base64 to original file bytes
            val fileBytes = try {
                Base64.decode(combinedData.toString(), Base64.DEFAULT)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode Base64 data", e)
                return@withContext null
            }

            Log.d(TAG, "Reconstructed ${fileBytes.size} bytes of original file data")
            return@withContext fileBytes

        } catch (e: Exception) {
            Log.e(TAG, "Error reconstructing file", e)
            return@withContext null
        }
    }

    /**
     * Reconstructs an image bitmap from file bytes (for preview only).
     *
     * @param fileBytes Original file bytes
     * @return Bitmap for preview, or null if decoding fails
     */
    suspend fun bytesToBitmap(fileBytes: ByteArray): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val bitmap = BitmapFactory.decodeByteArray(fileBytes, 0, fileBytes.size)
            if (bitmap == null) {
                Log.e(TAG, "Bitmap decoding returned null")
                return@withContext null
            }
            Log.d(TAG, "Preview bitmap created: ${bitmap.width}x${bitmap.height}")
            return@withContext bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode bitmap for preview", e)
            return@withContext null
        }
    }

    /**
     * Validates a chunk's checksum to ensure data integrity.
     *
     * @param chunk The QR chunk to validate
     * @return true if checksum is valid, false otherwise
     */
    private fun validateChecksum(chunk: QRChunk): Boolean {
        if (chunk.data == null) return false

        try {
            val md5 = MessageDigest.getInstance("MD5")
            val digest = md5.digest(chunk.data.toByteArray())
            // Take only first 4 bytes to match QRGenerator's checksum format
            val calculatedChecksum = digest.take(4)
                .joinToString("") { "%02x".format(it) }
                .uppercase()

            val isValid = calculatedChecksum.equals(chunk.checksum, ignoreCase = true)
            if (!isValid) {
                Log.w(TAG, "Checksum mismatch for chunk ${chunk.index}")
                Log.w(TAG, "Expected: ${chunk.checksum}, Got: $calculatedChecksum")
            }

            return isValid
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating checksum", e)
            return false
        }
    }

    /**
     * Estimates the completion percentage based on scanned chunks.
     *
     * @param scannedCount Number of chunks scanned
     * @param totalCount Total number of chunks expected
     * @return Percentage (0-100)
     */
    fun calculateProgress(scannedCount: Int, totalCount: Int): Int {
        if (totalCount == 0) return 0
        return ((scannedCount.toFloat() / totalCount) * 100).toInt()
    }
}
