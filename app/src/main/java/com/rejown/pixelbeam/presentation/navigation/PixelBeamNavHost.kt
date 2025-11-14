package com.rejown.pixelbeam.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.rejown.pixelbeam.presentation.home.HomeScreen
import com.rejown.pixelbeam.presentation.sender.picker.ImagePickerScreen
import com.rejown.pixelbeam.presentation.receiver.scanner.QRScannerScreen

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
                onReceiveClick = { navController.navigate(Screen.QRScanner) }
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

            // TODO: Implement ImagePreviewScreen
            // ImagePreviewScreen(
            //     imageUri = imagePreview.imageUri,
            //     onApprove = {
            //         // After compression and QR generation
            //         navController.navigate(Screen.QRDisplay) {
            //             // Remove ImagePreview from back stack
            //             popUpTo(Screen.ImagePicker) { inclusive = true }
            //         }
            //     },
            //     onCancel = {
            //         navController.popBackStack()
            //     }
            // )
        }

        composable<Screen.QRDisplay> {
            // TODO: Implement QRDisplayScreen
            // QRDisplayScreen(
            //     onComplete = {
            //         navController.navigate(Screen.Home) {
            //             popUpTo(Screen.Home) { inclusive = true }
            //         }
            //     },
            //     onBackPressed = {
            //         navController.popBackStack()
            //     }
            // )
        }

        // ============ RECEIVER FLOW ============

        composable<Screen.QRScanner> {
            QRScannerScreen(
                onBackPressed = {
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.ImageResult> {
            // TODO: Implement ImageResultScreen
            // ImageResultScreen(
            //     onSave = {
            //         navController.navigate(Screen.Home) {
            //             popUpTo(Screen.Home) { inclusive = true }
            //         }
            //     },
            //     onDiscard = {
            //         navController.navigate(Screen.Home) {
            //             popUpTo(Screen.Home) { inclusive = true }
            //         }
            //     },
            //     onBackPressed = {
            //         navController.popBackStack()
            //     }
            // )
        }
    }
}
