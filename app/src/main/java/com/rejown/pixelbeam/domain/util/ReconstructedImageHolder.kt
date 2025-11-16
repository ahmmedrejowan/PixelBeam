package com.rejown.pixelbeam.domain.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.rejown.pixelbeam.data.model.TransferMetadata
import java.io.File
import java.io.FileOutputStream

/**
 * Temporary holder for reconstructed file data between screens.
 *
 * Saves original file bytes to cache directory immediately to preserve file integrity.
 * The cached file is used for preview and save operations.
 */
object ReconstructedImageHolder {
    private const val TAG = "ReconstructedImageHolder"
    private const val CACHE_FILE_NAME = "received_file_temp"

    @Volatile
    private var _cacheFileUri: Uri? = null

    @Volatile
    private var _metadata: TransferMetadata? = null

    /**
     * The cached file URI, or null if none is stored
     */
    val imageUri: Uri?
        get() = _cacheFileUri

    /**
     * The transfer metadata associated with the reconstructed file
     */
    var metadata: TransferMetadata?
        get() = _metadata
        set(value) {
            _metadata = value
            Log.d(TAG, "Metadata stored: ${value?.filename}")
        }

    /**
     * Save original file bytes to cache and store metadata
     * @param fileBytes The original file bytes (no compression, no modification)
     * @param metadata Transfer metadata containing filename and other info
     */
    fun store(context: Context, fileBytes: ByteArray, metadata: TransferMetadata) {
        try {
            // Clear previous cache file
            clear(context)

            // Get file extension from filename
            val extension = metadata.filename.substringAfterLast('.', "jpg")
            val cacheFileName = "$CACHE_FILE_NAME.$extension"

            // Save original file bytes to cache (no modification)
            val cacheFile = File(context.cacheDir, cacheFileName)
            FileOutputStream(cacheFile).use { out ->
                out.write(fileBytes)
            }

            // Get content URI for the cache file
            _cacheFileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            _metadata = metadata

            Log.d(TAG, "Original file saved to cache: $cacheFile (${fileBytes.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Error storing file to cache", e)
        }
    }

    /**
     * Clears the cached file and metadata
     */
    fun clear(context: Context) {
        try {
            // Delete all cache files matching our pattern
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith(CACHE_FILE_NAME)) {
                    file.delete()
                    Log.d(TAG, "Cache file deleted: ${file.name}")
                }
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
