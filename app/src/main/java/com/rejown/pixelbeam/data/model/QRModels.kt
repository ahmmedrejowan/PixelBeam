package com.rejown.pixelbeam.data.model

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

/**
 * Represents a single QR code chunk in the transfer
 */
data class QRChunk(
    val index: Int,
    val total: Int,
    val content: String,
    val checksum: String,
    val data: String? = null,
    val isMetadata: Boolean = false,
    val metadata: TransferMetadata? = null,
    val qrBitmap: Bitmap? = null
)

/**
 * Metadata about the image transfer
 */
@Serializable
data class TransferMetadata(
    val filename: String,
    val originalSizeBytes: Long,
    val compressedSizeBytes: Long,
    val timestamp: Long,
    val mimeType: String = "image/jpeg",
    val totalChunks: Int,
    val fileChecksum: String = "" // SHA-256 checksum of entire original file
)
