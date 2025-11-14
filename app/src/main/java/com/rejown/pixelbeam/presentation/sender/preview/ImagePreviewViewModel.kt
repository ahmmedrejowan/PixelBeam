package com.rejown.pixelbeam.presentation.sender.preview

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejown.pixelbeam.domain.util.CompressedImage
import com.rejown.pixelbeam.domain.util.ImageCompressor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class CompressionState {
    data object Idle : CompressionState()
    data object Loading : CompressionState()
    data class Success(val compressedImage: CompressedImage) : CompressionState()
    data class Error(val message: String) : CompressionState()
}

data class ImagePreviewState(
    val imageUri: Uri? = null,
    val compressionState: CompressionState = CompressionState.Idle,
    val isProcessing: Boolean = false
)

class ImagePreviewViewModel(
    private val imageCompressor: ImageCompressor
) : ViewModel() {

    private val _state = MutableStateFlow(ImagePreviewState())
    val state: StateFlow<ImagePreviewState> = _state.asStateFlow()

    fun loadImage(uri: Uri) {
        _state.update { it.copy(imageUri = uri) }
        compressImage(uri)
    }

    private fun compressImage(uri: Uri) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    compressionState = CompressionState.Loading,
                    isProcessing = true
                )
            }

            try {
                val compressedImage = imageCompressor.compressImage(uri)
                if (compressedImage != null) {
                    _state.update {
                        it.copy(
                            compressionState = CompressionState.Success(compressedImage),
                            isProcessing = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            compressionState = CompressionState.Error("Failed to compress image"),
                            isProcessing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        compressionState = CompressionState.Error(
                            e.message ?: "Unknown error occurred"
                        ),
                        isProcessing = false
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up bitmaps
        val compressionState = _state.value.compressionState
        if (compressionState is CompressionState.Success) {
            compressionState.compressedImage.originalBitmap.recycle()
            compressionState.compressedImage.compressedBitmap.recycle()
        }
    }
}
