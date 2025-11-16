package com.rejown.pixelbeam.presentation.receiver.scanner

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejown.pixelbeam.data.model.QRChunk
import com.rejown.pixelbeam.data.model.TransferMetadata
import com.rejown.pixelbeam.domain.util.ImageReconstructor
import com.rejown.pixelbeam.domain.util.ReconstructedImageHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.MessageDigest

sealed class ScanState {
    data object Idle : ScanState()
    data class Scanning(
        val scannedChunks: Set<Int>,
        val totalChunks: Int,
        val metadata: TransferMetadata?
    ) : ScanState()
    data object Reconstructing : ScanState()
    data class Success(val bitmap: Bitmap, val metadata: TransferMetadata) : ScanState()
    data class Error(val message: String) : ScanState()
}

data class QRScannerState(
    val scanState: ScanState = ScanState.Idle,
    val lastScannedIndex: Int? = null,
    val hasCameraPermission: Boolean = false
)

class QRScannerViewModel(
    private val imageReconstructor: ImageReconstructor,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(QRScannerState())
    val state: StateFlow<QRScannerState> = _state.asStateFlow()

    private val scannedChunks = mutableMapOf<Int, QRChunk>()
    private var metadata: TransferMetadata? = null
    private var totalChunks: Int = 0

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "QRScannerViewModel"
    }

    fun setCameraPermission(granted: Boolean) {
        _state.update { it.copy(hasCameraPermission = granted) }
    }

    fun onQRCodeDetected(qrContent: String) {
        viewModelScope.launch {
            try {
                val chunk = parseQRContent(qrContent) ?: return@launch

                // Check if we already scanned this chunk
                if (scannedChunks.containsKey(chunk.index)) {
                    Log.d(TAG, "Duplicate chunk ${chunk.index}, skipping")
                    return@launch
                }

                // Handle metadata chunk
                if (chunk.isMetadata && chunk.metadata != null) {
                    metadata = chunk.metadata
                    totalChunks = chunk.metadata.totalChunks
                    Log.d(TAG, "Metadata received: $totalChunks chunks total")
                }

                // Add chunk to collection
                scannedChunks[chunk.index] = chunk
                Log.d(TAG, "Scanned chunk ${chunk.index + 1} / $totalChunks")

                // Update state
                _state.update {
                    it.copy(
                        scanState = ScanState.Scanning(
                            scannedChunks = scannedChunks.keys,
                            totalChunks = totalChunks,
                            metadata = metadata
                        ),
                        lastScannedIndex = chunk.index
                    )
                }

                // Check if we have all chunks
                if (totalChunks > 0 && scannedChunks.size == totalChunks) {
                    reconstructImage()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing QR code", e)
            }
        }
    }

    private fun parseQRContent(content: String): QRChunk? {
        try {
            // Format: IMG|TOTAL|INDEX|CHECKSUM|DATA
            val parts = content.split("|")
            if (parts.size < 4 || parts[0] != "IMG") {
                Log.w(TAG, "Invalid QR format")
                return null
            }

            val total = parts[1].toInt()
            val index = parts[2].toInt()
            val checksum = parts[3]
            val data = if (parts.size > 4) parts[4] else null

            // Check if this is metadata chunk
            val isMetadata = data?.startsWith("{") == true
            val metadata = if (isMetadata) {
                try {
                    json.decodeFromString<TransferMetadata>(data!!)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse metadata", e)
                    null
                }
            } else null

            return QRChunk(
                index = index,
                total = total,
                content = content,
                checksum = checksum,
                data = data,
                isMetadata = isMetadata,
                metadata = metadata
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing QR content", e)
            return null
        }
    }

    private suspend fun reconstructImage() {
        try {
            _state.update { it.copy(scanState = ScanState.Reconstructing) }
            Log.d(TAG, "Starting file reconstruction")

            // Sort chunks by index
            val sortedChunks = scannedChunks.values.sortedBy { it.index }

            // Reconstruct original file bytes (exact copy, no modification)
            val fileBytes = imageReconstructor.reconstructImageBytes(sortedChunks)

            if (fileBytes != null && metadata != null) {
                // Verify file integrity with checksum
                val receivedChecksum = calculateSHA256(fileBytes)
                val expectedChecksum = metadata!!.fileChecksum

                Log.d(TAG, "Checksum verification:")
                Log.d(TAG, "  Expected: $expectedChecksum")
                Log.d(TAG, "  Received: $receivedChecksum")

                if (expectedChecksum.isNotEmpty() && receivedChecksum != expectedChecksum) {
                    _state.update {
                        it.copy(scanState = ScanState.Error("File integrity check failed! Checksums don't match."))
                    }
                    Log.e(TAG, "Checksum mismatch! File may be corrupted.")
                    return
                }

                Log.d(TAG, "✓ Checksum verified - file is intact")

                // Store original file bytes in cache
                ReconstructedImageHolder.store(context, fileBytes, metadata!!)

                // Create bitmap for preview only (not saved)
                val bitmap = imageReconstructor.bytesToBitmap(fileBytes)

                if (bitmap != null) {
                    _state.update {
                        it.copy(scanState = ScanState.Success(bitmap, metadata!!))
                    }
                    Log.d(TAG, "File reconstruction successful - original file saved and verified")
                } else {
                    _state.update {
                        it.copy(scanState = ScanState.Error("Failed to create preview bitmap"))
                    }
                }
            } else {
                _state.update {
                    it.copy(scanState = ScanState.Error("Failed to reconstruct file"))
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error reconstructing image", e)
            _state.update {
                it.copy(scanState = ScanState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun getMissingChunks(): List<Int> {
        if (totalChunks == 0) return emptyList()
        return (0 until totalChunks).filter { !scannedChunks.containsKey(it) }
    }

    fun resetScan() {
        scannedChunks.clear()
        metadata = null
        totalChunks = 0
        _state.update {
            it.copy(
                scanState = ScanState.Idle,
                lastScannedIndex = null
            )
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

    override fun onCleared() {
        super.onCleared()
        // Clean up any bitmaps if needed
        val currentState = _state.value.scanState
        if (currentState is ScanState.Success && !currentState.bitmap.isRecycled) {
            currentState.bitmap.recycle()
        }
    }
}
