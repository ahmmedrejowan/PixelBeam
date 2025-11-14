package com.rejown.pixelbeam.presentation.sender.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.view.WindowManager
import androidx.activity.ComponentActivity
import com.rejown.pixelbeam.domain.util.TempDataHolder
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRDisplayScreen(
    onComplete: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QRDisplayViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Keep screen awake during QR display
    DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Load compressed image and generate QR codes
    LaunchedEffect(Unit) {
        TempDataHolder.compressedImage?.let { compressedImage ->
            viewModel.generateQRCodes(compressedImage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Code Transfer") },
                navigationIcon = {
                    IconButton(onClick = {
                        TempDataHolder.clear()
                        onBackPressed()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        when (val generationState = state.generationState) {
            is QRGenerationState.Generating -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        Text(
                            text = "Generating QR codes...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (generationState.total > 0) {
                            Text(
                                text = "${generationState.progress} / ${generationState.total}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            is QRGenerationState.Success -> {
                val chunks = generationState.chunks
                val currentChunk = chunks.getOrNull(state.currentIndex)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "QR Code ${state.currentIndex + 1} of ${chunks.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isAutoAdvancing) "Auto" else "Manual",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (state.currentIndex + 1).toFloat() / chunks.size },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // QR Code Display
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            currentChunk?.qrBitmap?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "QR Code ${state.currentIndex + 1}",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } ?: Text("No QR code available")
                        }
                    }

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.previousQR() },
                            enabled = state.currentIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, "Previous")
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.toggleAutoAdvance() }
                        ) {
                            Icon(
                                if (state.isAutoAdvancing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                if (state.isAutoAdvancing) "Pause" else "Play"
                            )
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.nextQR() },
                            enabled = state.currentIndex < chunks.size - 1
                        ) {
                            Icon(Icons.Default.ArrowForward, "Next")
                        }
                    }

                    // Complete button
                    if (state.currentIndex == chunks.size - 1) {
                        Button(
                            onClick = {
                                TempDataHolder.clear()
                                onComplete()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Transfer Complete - Done")
                        }
                    }
                }
            }

            is QRGenerationState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(generationState.message)
                        Button(onClick = onBackPressed) {
                            Text("Go Back")
                        }
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Initializing...")
                }
            }
        }
    }
}
