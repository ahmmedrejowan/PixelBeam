package com.rejown.pixelbeam.domain.util

import android.graphics.Bitmap
import android.util.Log
import com.rejown.pixelbeam.data.model.TransferMetadata

/**
 * Temporary holder for reconstructed image data between screens.
 *
 * This is a singleton holder that temporarily stores the reconstructed Bitmap and
 * metadata during the receiver flow navigation (QRScanner -> ImageResult).
 *
 * Design considerations:
 * - Bitmaps are too large for SavedStateHandle (~1MB limit)
 * - Shared ViewModels require complex navigation graph scoping
 * - This singleton approach is simple and effective for short-lived data
 *
 * Memory management:
 * - Data is stored when QRScannerViewModel completes reconstruction
 * - Data is retrieved when ImageResultScreen launches
 * - Data is cleared when user saves/discards the image
 * - Bitmaps are properly recycled to prevent memory leaks
 *
 * Thread safety: This holder is accessed from the main thread only (UI operations)
 */
object ReconstructedImageHolder {
    private const val TAG = "ReconstructedImageHolder"

    @Volatile
    private var _bitmap: Bitmap? = null

    @Volatile
    private var _metadata: TransferMetadata? = null

    /**
     * The reconstructed bitmap, or null if none is stored
     */
    var bitmap: Bitmap?
        get() = _bitmap
        set(value) {
            // Clear previous bitmap before setting new one
            if (_bitmap != null && _bitmap != value) {
                clearBitmap()
            }
            _bitmap = value
            Log.d(TAG, "Bitmap stored: ${value?.width}x${value?.height}")
        }

    /**
     * The transfer metadata associated with the reconstructed image
     */
    var metadata: TransferMetadata?
        get() = _metadata
        set(value) {
            _metadata = value
            Log.d(TAG, "Metadata stored: ${value?.filename}")
        }

    /**
     * Clears the stored bitmap and metadata, recycling the bitmap to free memory
     */
    fun clear() {
        clearBitmap()
        _metadata = null
        Log.d(TAG, "ReconstructedImageHolder cleared")
    }

    private fun clearBitmap() {
        _bitmap?.let { bmp ->
            try {
                if (!bmp.isRecycled) {
                    bmp.recycle()
                    Log.d(TAG, "Bitmap recycled")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error recycling bitmap", e)
            }
        }
        _bitmap = null
    }

    /**
     * Check if data is currently stored
     */
    fun hasData(): Boolean = _bitmap != null && _metadata != null

    /**
     * Store both bitmap and metadata together
     */
    fun store(bitmap: Bitmap, metadata: TransferMetadata) {
        this.bitmap = bitmap
        this.metadata = metadata
    }
}
