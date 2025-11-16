package com.rejown.pixelbeam.presentation.sender.display

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                    // Progress header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "QR Code ${state.currentIndex + 1}/${chunks.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (state.isAutoAdvancing) "Auto-playing" else if (state.currentIndex == 0 && !state.hasStarted) "Tap Start" else "Manual",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isAutoAdvancing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Speed selector chip
                        var speedMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            FilledTonalButton(
                                onClick = { speedMenuExpanded = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDelay(state.autoAdvanceDelayMs),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            DropdownMenu(
                                expanded = speedMenuExpanded,
                                onDismissRequest = { speedMenuExpanded = false }
                            ) {
                                state.availableDelays.forEach { delay ->
                                    DropdownMenuItem(
                                        text = { Text(formatDelay(delay)) },
                                        onClick = {
                                            viewModel.setAutoAdvanceDelay(delay)
                                            speedMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (state.currentIndex + 1).toFloat() / chunks.size },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Square QR Code Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
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

                    // Slider
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Slider(
                            value = state.currentIndex.toFloat(),
                            onValueChange = { viewModel.setCurrentIndex(it.toInt()) },
                            valueRange = 0f..(chunks.size - 1).toFloat(),
                            steps = if (chunks.size > 2) chunks.size - 2 else 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "#${state.currentIndex + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${chunks.size} total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Controls
                    if (!state.hasStarted && state.currentIndex == 0) {
                        // Initial state - Big Start button
                        Button(
                            onClick = { viewModel.startTransfer() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Transfer",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        // After started - Show all controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousQR() },
                                enabled = state.currentIndex > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Previous",
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Button(
                                onClick = { viewModel.toggleAutoAdvance() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            ) {
                                Icon(
                                    if (state.isAutoAdvancing) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (state.isAutoAdvancing) "Pause" else "Resume",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            IconButton(
                                onClick = { viewModel.nextQR() },
                                enabled = state.currentIndex < chunks.size - 1,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    "Next",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Finish button - shown after reaching the end at least once
                    if (state.hasReachedEnd) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                TempDataHolder.clear()
                                onComplete()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                        ) {
                            Text(
                                text = "Finish Transfer",
                                style = MaterialTheme.typography.titleMedium
                            )
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

/**
 * Helper function to format delay in milliseconds to readable string
 */
private fun formatDelay(delayMs: Long): String {
    return when {
        delayMs < 1000 -> "${delayMs}ms"
        delayMs % 1000 == 0L -> "${delayMs / 1000}s"
        else -> "${delayMs / 1000.0}s"
    }
}
