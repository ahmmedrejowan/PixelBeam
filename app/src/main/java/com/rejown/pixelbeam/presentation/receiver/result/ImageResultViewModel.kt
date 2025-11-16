package com.rejown.pixelbeam.presentation.receiver.result

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rejown.pixelbeam.data.model.TransferMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data class Success(val uri: Uri) : SaveState()
    data class Error(val message: String) : SaveState()
}

data class ImageResultState(
    val saveState: SaveState = SaveState.Idle,
    val imageUri: Uri? = null,
    val metadata: TransferMetadata? = null
)

class ImageResultViewModel(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ImageResultState())
    val state: StateFlow<ImageResultState> = _state.asStateFlow()

    companion object {
        private const val TAG = "ImageResultViewModel"
    }

    fun setImageData(imageUri: Uri, metadata: TransferMetadata) {
        _state.update {
            it.copy(
                imageUri = imageUri,
                metadata = metadata
            )
        }
    }

    fun saveToUri(targetUri: Uri) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(saveState = SaveState.Saving) }

                val sourceUri = _state.value.imageUri

                if (sourceUri == null) {
                    _state.update {
                        it.copy(saveState = SaveState.Error("No image to save"))
                    }
                    return@launch
                }

                val success = copyImageToUri(sourceUri, targetUri)

                if (success) {
                    _state.update { it.copy(saveState = SaveState.Success(targetUri)) }
                    Log.d(TAG, "Image saved: $targetUri")
                } else {
                    _state.update {
                        it.copy(saveState = SaveState.Error("Failed to save image"))
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving image", e)
                _state.update {
                    it.copy(saveState = SaveState.Error(e.message ?: "Unknown error"))
                }
            }
        }
    }

    private suspend fun copyImageToUri(sourceUri: Uri, targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                    inputStream.copyTo(outputStream)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to URI", e)
            false
        }
    }

    fun resetSaveState() {
        _state.update { it.copy(saveState = SaveState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        // Cache file cleanup is handled by ReconstructedImageHolder
    }
}
