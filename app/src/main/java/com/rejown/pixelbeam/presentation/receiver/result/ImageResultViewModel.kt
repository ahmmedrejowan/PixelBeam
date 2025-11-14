package com.rejown.pixelbeam.presentation.receiver.result

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import java.io.IOException

sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data class Success(val uri: Uri) : SaveState()
    data class Error(val message: String) : SaveState()
}

data class ImageResultState(
    val saveState: SaveState = SaveState.Idle,
    val bitmap: Bitmap? = null,
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

    fun setImageData(bitmap: Bitmap, metadata: TransferMetadata) {
        _state.update {
            it.copy(
                bitmap = bitmap,
                metadata = metadata
            )
        }
    }

    fun saveToGallery() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(saveState = SaveState.Saving) }

                val bitmap = _state.value.bitmap
                val metadata = _state.value.metadata

                if (bitmap == null) {
                    _state.update {
                        it.copy(saveState = SaveState.Error("No image to save"))
                    }
                    return@launch
                }

                val uri = saveImageToGallery(bitmap, metadata?.filename ?: "pixelbeam_${System.currentTimeMillis()}.jpg")

                if (uri != null) {
                    _state.update { it.copy(saveState = SaveState.Success(uri)) }
                    Log.d(TAG, "Image saved to gallery: $uri")
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

    private suspend fun saveImageToGallery(bitmap: Bitmap, filename: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PixelBeam")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val contentResolver = context.contentResolver
            val imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            imageUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Failed to compress bitmap")
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }

                return@withContext uri
            }

            return@withContext null

        } catch (e: Exception) {
            Log.e(TAG, "Error saving to gallery", e)
            return@withContext null
        }
    }

    fun resetSaveState() {
        _state.update { it.copy(saveState = SaveState.Idle) }
    }

    override fun onCleared() {
        super.onCleared()
        // Bitmap cleanup is handled by ReconstructedImageHolder
    }
}
