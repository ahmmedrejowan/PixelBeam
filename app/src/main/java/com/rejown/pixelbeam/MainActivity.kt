package com.rejown.pixelbeam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.rejown.pixelbeam.presentation.navigation.PixelBeamNavHost
import com.rejown.pixelbeam.ui.theme.PixelBeamTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PixelBeamTheme {
                PixelBeamApp()
            }
        }
    }
}

@Composable
fun PixelBeamApp() {
    val navController = rememberNavController()

    PixelBeamNavHost(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}