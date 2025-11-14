package com.rejown.pixelbeam.domain.util

import android.util.Log

/**
 * Temporary holder for compressed image data between screens.
 *
 * This is a singleton holder that temporarily stores CompressedImage data during
 * the sender flow navigation (ImagePreview -> QRDisplay). Since Jetpack Compose
 * Navigation cannot pass large objects like Bitmaps through routes, this provides
 * a safe way to share the data between screens.
 *
 * Design considerations:
 * - Bitmaps are too large for SavedStateHandle (~1MB limit)
 * - Shared ViewModels require complex navigation graph scoping
 * - This singleton approach is simple and effective for short-lived data
 *
 * Memory management:
 * - Data is stored when user approves compression in ImagePreviewScreen
 * - Data is retrieved when QRDisplayScreen launches
 * - Data is cleared when user navigates back or completes transfer
 * - Bitmaps are properly recycled to prevent memory leaks
 *
 * Thread safety: This holder is accessed from the main thread only (UI operations)
 */
object TempDataHolder {
    private const val TAG = "TempDataHolder"

    @Volatile
    private var _compressedImage: CompressedImage? = null

    /**
     * The currently stored compressed image, or null if none is stored
     */
    var compressedImage: CompressedImage?
        get() = _compressedImage
        set(value) {
            // Clear previous data before setting new one
            if (_compressedImage != null && _compressedImage != value) {
                clear()
            }
            _compressedImage = value
            Log.d(TAG, "CompressedImage stored: ${value?.compressedSizeKB} KB")
        }

    /**
     * Clears the stored compressed image and recycles bitmaps to free memory
     */
    fun clear() {
        _compressedImage?.let { image ->
            try {
                if (!image.originalBitmap.isRecycled) {
                    image.originalBitmap.recycle()
                }
                if (!image.compressedBitmap.isRecycled) {
                    image.compressedBitmap.recycle()
                }
                Log.d(TAG, "CompressedImage cleared and bitmaps recycled")
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling bitmaps", e)
            }
        }
        _compressedImage = null
    }

    /**
     * Check if data is currently stored
     */
    fun hasData(): Boolean = _compressedImage != null
}
