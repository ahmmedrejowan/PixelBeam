package com.rejown.pixelbeam.domain.util

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.rejown.pixelbeam.data.model.QRChunk
import com.rejown.pixelbeam.data.model.TransferMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Utility for generating QR codes from image data
 */
class QRGenerator {

    companion object {
        private const val QR_SIZE = 512
        private const val CHUNK_SIZE = 2800 // Safe size for QR codes with error correction
        private const val HEADER = "IMG"
    }

    /**
     * Generate QR code bitmap from text content
     */
    suspend fun generateQRCode(content: String): Bitmap? = withContext(Dispatchers.Default) {
        try {
            val hints = hashMapOf<EncodeHintType, Any>().apply {
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.MARGIN, 1)
            }

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(
                content,
                BarcodeFormat.QR_CODE,
                QR_SIZE,
                QR_SIZE,
                hints
            )

            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
                }
            }

            Bitmap.createBitmap(
                pixels,
                width,
                height,
                Bitmap.Config.RGB_565
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Prepare image data for QR codes
     * Returns list of QR chunks with metadata
     */
    suspend fun prepareQRChunks(
        compressedBytes: ByteArray,
        metadata: TransferMetadata
    ): List<QRChunk> = withContext(Dispatchers.Default) {
        val chunks = mutableListOf<QRChunk>()

        // Base64 encode the compressed image data
        val base64 = Base64.encodeToString(
            compressedBytes,
            Base64.NO_WRAP
        )

        // Split into chunks
        val totalChunks = (base64.length + CHUNK_SIZE - 1) / CHUNK_SIZE

        for (i in 0 until totalChunks) {
            val start = i * CHUNK_SIZE
            val end = minOf(start + CHUNK_SIZE, base64.length)
            val chunkData = base64.substring(start, end)

            // Calculate checksum for this chunk
            val checksum = calculateChecksum(chunkData)

            // Format: IMG|TOTAL|INDEX|CHECKSUM|DATA
            val qrContent = buildString {
                append(HEADER)
                append("|")
                append(totalChunks.toString().padStart(3, '0'))
                append("|")
                append(i.toString().padStart(3, '0'))
                append("|")
                append(checksum)
                append("|")
                append(chunkData)
            }

            chunks.add(
                QRChunk(
                    index = i,
                    total = totalChunks,
                    content = qrContent,
                    checksum = checksum,
                    data = chunkData
                )
            )
        }

        // Add metadata as the last chunk
        val metadataJson = Json.encodeToString(metadata)
        val metadataContent = "META|$metadataJson"

        chunks.add(
            QRChunk(
                index = totalChunks,
                total = totalChunks + 1,
                content = metadataContent,
                checksum = calculateChecksum(metadataJson),
                isMetadata = true,
                metadata = metadata
            )
        )

        chunks
    }

    /**
     * Generate QR code bitmaps for all chunks
     */
    suspend fun generateQRCodesForChunks(chunks: List<QRChunk>): List<QRChunk> =
        withContext(Dispatchers.Default) {
            chunks.map { chunk ->
                val qrBitmap = generateQRCode(chunk.content)
                chunk.copy(qrBitmap = qrBitmap)
            }
        }

    /**
     * Calculate MD5 checksum for data validation
     */
    private fun calculateChecksum(data: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data.toByteArray())
        return digest.take(4)
            .joinToString("") { "%02x".format(it) }
            .uppercase()
    }
}
