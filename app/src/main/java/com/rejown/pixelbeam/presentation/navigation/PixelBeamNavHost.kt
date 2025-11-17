package com.rejown.pixelbeam.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rejown.pixelbeam.presentation.about.AboutScreen
import com.rejown.pixelbeam.presentation.home.HomeScreen
import com.rejown.pixelbeam.presentation.sender.picker.ImagePickerScreen
import com.rejown.pixelbeam.presentation.sender.preview.ImagePreviewScreen
import com.rejown.pixelbeam.presentation.sender.display.QRDisplayScreen
import com.rejown.pixelbeam.presentation.receiver.scanner.QRScannerScreen
import com.rejown.pixelbeam.presentation.receiver.result.ImageResultScreen

/**
 * Main navigation host for PixelBeam app
 * Handles all screen navigation using type-safe routes
 *
 * @param navController The navigation controller for managing navigation
 * @param modifier Optional modifier for the NavHost
 */
@Composable
fun PixelBeamNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = modifier
    ) {
        // ============ HOME SCREEN ============
        composable<Screen.Home> {
            HomeScreen(
                onSendClick = { navController.navigate(Screen.ImagePicker) },
                onReceiveClick = { navController.navigate(Screen.QRScanner) },
                onAboutClick = { navController.navigate(Screen.About) }
            )
        }

        // ============ ABOUT SCREEN ============
        composable<Screen.About> {
            AboutScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // ============ SENDER FLOW ============

        composable<Screen.ImagePicker> {
            ImagePickerScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onImageSelected = { uri ->
                    navController.navigate(Screen.ImagePreview(imageUri = uri.toString()))
                }
            )
        }

        composable<Screen.ImagePreview> { backStackEntry ->
            val imagePreview = backStackEntry.toRoute<Screen.ImagePreview>()

            ImagePreviewScreen(
                imageUri = imagePreview.imageUri,
                onApprove = {
                    // After compression, navigate to QR display
                    navController.navigate(Screen.QRDisplay) {
                        // Remove ImagePreview and ImagePicker from back stack
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.QRDisplay> {
            QRDisplayScreen(
                onComplete = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onBackPressed = {
                    navController.popBackStack(Screen.Home, inclusive = false)
                }
            )
        }

        // ============ RECEIVER FLOW ============

        composable<Screen.QRScanner> {
            QRScannerScreen(
                onBackPressed = {
                    navController.popBackStack()
                },
                onScanComplete = {
                    navController.navigate(Screen.ImageResult) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                }
            )
        }

        composable<Screen.ImageResult> {
            ImageResultScreen(
                onSave = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                },
                onDiscard = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                    }
                }
            )
        }
    }
}
