package com.rejown.pixelbeam.presentation.sender.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejown.pixelbeam.data.model.QRChunk
import com.rejown.pixelbeam.data.model.TransferMetadata
import com.rejown.pixelbeam.domain.util.CompressedImage
import com.rejown.pixelbeam.domain.util.QRGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class QRGenerationState {
    data object Idle : QRGenerationState()
    data class Generating(val progress: Int, val total: Int) : QRGenerationState()
    data class Success(val chunks: List<QRChunk>) : QRGenerationState()
    data class Error(val message: String) : QRGenerationState()
}

data class QRDisplayState(
    val generationState: QRGenerationState = QRGenerationState.Idle,
    val currentIndex: Int = 0,
    val isAutoAdvancing: Boolean = false,
    val hasStarted: Boolean = false,
    val hasReachedEnd: Boolean = false,
    val autoAdvanceDelayMs: Long = 1000,
    val availableDelays: List<Long> = listOf(50L, 100L, 150L, 200L, 300L, 400L, 500L, 1000L, 1500L, 2000L, 3000L, 5000L)
)

class QRDisplayViewModel(
    private val qrGenerator: QRGenerator
) : ViewModel() {

    private val _state = MutableStateFlow(QRDisplayState())
    val state: StateFlow<QRDisplayState> = _state.asStateFlow()

    fun generateQRCodes(compressedImage: CompressedImage) {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(generationState = QRGenerationState.Generating(0, 0))
                }

                // Create metadata
                val metadata = TransferMetadata(
                    filename = compressedImage.filename,
                    originalSizeBytes = compressedImage.originalSizeBytes,
                    compressedSizeBytes = compressedImage.compressedSizeBytes,
                    timestamp = System.currentTimeMillis(),
                    mimeType = compressedImage.mimeType,
                    totalChunks = 0, // Will be updated
                    fileChecksum = compressedImage.fileChecksum
                )

                // Prepare chunks
                val chunks = qrGenerator.prepareQRChunks(
                    compressedImage.compressedBytes,
                    metadata.copy(totalChunks = 0)
                )

                // Update progress
                _state.update {
                    it.copy(
                        generationState = QRGenerationState.Generating(
                            progress = 0,
                            total = chunks.size
                        )
                    )
                }

                // Generate QR bitmaps for all chunks
                val chunksWithQR = qrGenerator.generateQRCodesForChunks(chunks)

                _state.update {
                    it.copy(
                        generationState = QRGenerationState.Success(chunksWithQR)
                    )
                }
                // Don't auto-start - wait for user to click Start button

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        generationState = QRGenerationState.Error(
                            e.message ?: "Failed to generate QR codes"
                        )
                    )
                }
            }
        }
    }

    fun nextQR() {
        val currentState = _state.value
        val generationState = currentState.generationState

        if (generationState is QRGenerationState.Success) {
            val chunks = generationState.chunks
            if (currentState.currentIndex < chunks.size - 1) {
                val newIndex = currentState.currentIndex + 1
                _state.update {
                    it.copy(
                        currentIndex = newIndex,
                        hasReachedEnd = it.hasReachedEnd || newIndex == chunks.size - 1
                    )
                }
            }
        }
    }

    fun previousQR() {
        val currentState = _state.value
        if (currentState.currentIndex > 0) {
            // Preserve hasReachedEnd flag - once reached, it stays true
            _state.update { it.copy(currentIndex = it.currentIndex - 1) }
        }
    }

    fun setCurrentIndex(index: Int) {
        val currentState = _state.value
        val generationState = currentState.generationState
        if (generationState is QRGenerationState.Success) {
            val maxIndex = generationState.chunks.size - 1
            val validIndex = index.coerceIn(0, maxIndex)
            _state.update {
                it.copy(
                    currentIndex = validIndex,
                    hasReachedEnd = currentState.hasReachedEnd || validIndex == maxIndex
                )
            }
        }
    }

    fun startTransfer() {
        _state.update { it.copy(hasStarted = true, isAutoAdvancing = true) }
        viewModelScope.launch {
            startAutoAdvance()
        }
    }

    fun toggleAutoAdvance() {
        _state.update { it.copy(isAutoAdvancing = !it.isAutoAdvancing, hasStarted = true) }

        if (_state.value.isAutoAdvancing) {
            viewModelScope.launch {
                startAutoAdvance()
            }
        }
    }

    fun setAutoAdvanceDelay(delayMs: Long) {
        _state.update { it.copy(autoAdvanceDelayMs = delayMs) }
    }

    private suspend fun startAutoAdvance() {
        while (_state.value.isAutoAdvancing) {
            delay(_state.value.autoAdvanceDelayMs)

            val currentState = _state.value
            val generationState = currentState.generationState

            if (generationState is QRGenerationState.Success) {
                val chunks = generationState.chunks
                if (currentState.currentIndex < chunks.size - 1) {
                    val newIndex = currentState.currentIndex + 1
                    _state.update {
                        it.copy(
                            currentIndex = newIndex,
                            hasReachedEnd = it.hasReachedEnd || newIndex == chunks.size - 1
                        )
                    }
                } else {
                    // Reached end, stop auto-advance
                    _state.update { it.copy(isAutoAdvancing = false, hasReachedEnd = true) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up QR bitmaps
        val generationState = _state.value.generationState
        if (generationState is QRGenerationState.Success) {
            generationState.chunks.forEach { chunk ->
                chunk.qrBitmap?.recycle()
            }
        }
    }
}
