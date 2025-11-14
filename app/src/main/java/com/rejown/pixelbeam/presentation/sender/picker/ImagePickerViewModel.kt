package com.rejown.pixelbeam.presentation.sender.picker

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ImagePickerState(
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class ImagePickerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ImagePickerState())
    val state: StateFlow<ImagePickerState> = _state.asStateFlow()

    fun onImageSelected(uri: Uri?) {
        _state.update { it.copy(selectedImageUri = uri, error = null) }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedImageUri = null, error = null) }
    }

    fun setError(message: String) {
        _state.update { it.copy(error = message, isLoading = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
