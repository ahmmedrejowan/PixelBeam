package com.rejown.pixelbeam.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Sealed class defining all navigation routes in the app
 * Uses kotlinx-serialization for type-safe navigation with Navigation Compose
 */
@Serializable
sealed class Screen {

    /**
     * Home screen - Entry point of the app
     * Shows options to Send or Receive images
     */
    @Serializable
    data object Home : Screen()

    // ============ SENDER FLOW ============

    /**
     * Image picker screen - Select image from gallery
     */
    @Serializable
    data object ImagePicker : Screen()

    /**
     * Image preview screen - Shows original and compressed image preview
     * @param imageUri The URI of the selected image
     */
    @Serializable
    data class ImagePreview(
        val imageUri: String
    ) : Screen()

    /**
     * QR display screen - Shows QR codes in sequence for scanning
     * Navigation happens after image processing in ImagePreview
     */
    @Serializable
    data object QRDisplay : Screen()

    // ============ RECEIVER FLOW ============

    /**
     * QR scanner screen - Camera view to scan QR codes
     */
    @Serializable
    data object QRScanner : Screen()

    /**
     * Image result screen - Shows reconstructed image with save option
     * Navigation happens after all QR codes are scanned and processed
     */
    @Serializable
    data object ImageResult : Screen()
}
