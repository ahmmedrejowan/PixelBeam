package com.rejown.pixelbeam.domain.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.rejown.pixelbeam.data.model.TransferMetadata
import java.io.File
import java.io.FileOutputStream

/**
 * Temporary holder for reconstructed image data between screens.
 *
 * Now saves images to cache directory immediately to avoid bitmap recycling issues.
 * The cached file is used for preview and save operations.
 */
object ReconstructedImageHolder {
    private const val TAG = "ReconstructedImageHolder"
    private const val CACHE_FILE_NAME = "received_image_temp.jpg"

    @Volatile
    private var _cacheFileUri: Uri? = null

    @Volatile
    private var _metadata: TransferMetadata? = null

    /**
     * The cached image file URI, or null if none is stored
     */
    val imageUri: Uri?
        get() = _cacheFileUri

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
     * Save bitmap to cache and store metadata
     */
    fun store(context: Context, bitmap: Bitmap, metadata: TransferMetadata) {
        try {
            // Clear previous cache file
            clear(context)

            // Save bitmap to cache
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            FileOutputStream(cacheFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            // Get content URI for the cache file
            _cacheFileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            _metadata = metadata

            // Recycle the bitmap now that it's saved
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            Log.d(TAG, "Image saved to cache: $cacheFile")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing image to cache", e)
        }
    }

    /**
     * Clears the cached file and metadata
     */
    fun clear(context: Context) {
        try {
            val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
            if (cacheFile.exists()) {
                cacheFile.delete()
                Log.d(TAG, "Cache file deleted")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cache file", e)
        }
        _cacheFileUri = null
        _metadata = null
        Log.d(TAG, "ReconstructedImageHolder cleared")
    }

    /**
     * Check if data is currently stored
     */
    fun hasData(): Boolean = _cacheFileUri != null && _metadata != null
}
