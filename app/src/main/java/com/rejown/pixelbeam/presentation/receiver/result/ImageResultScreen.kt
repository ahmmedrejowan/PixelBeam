package com.rejown.pixelbeam.presentation.receiver.result

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rejown.pixelbeam.domain.util.ReconstructedImageHolder
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageResultScreen(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageResultViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // File picker launcher for saving image
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/*")
    ) { uri ->
        if (uri != null) {
            viewModel.saveToUri(uri)
        }
    }

    // Load image from holder
    LaunchedEffect(Unit) {
        val imageUri = ReconstructedImageHolder.imageUri
        val metadata = ReconstructedImageHolder.metadata

        if (imageUri != null && metadata != null) {
            viewModel.setImageData(imageUri, metadata)
        }
    }

    // Clean up when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            // Note: Cache file is cleared after save/discard action
        }
    }

    // Handle save success
    LaunchedEffect(state.saveState) {
        if (state.saveState is SaveState.Success) {
            Toast.makeText(context, "Image saved!", Toast.LENGTH_LONG).show()
            ReconstructedImageHolder.clear(context)
            onSave()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Received Image") },
                navigationIcon = {
                    IconButton(onClick = {
                        ReconstructedImageHolder.clear(context)
                        onDiscard()
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
        if (state.imageUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No Image Data",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "No received image found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                ReconstructedImageHolder.clear(context)
                                onDiscard()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Go Back",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Metadata card
                state.metadata?.let { metadata ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Transfer Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            InfoRow(
                                label = "File Name",
                                value = metadata.filename
                            )

                            InfoRow(
                                label = "File Size",
                                value = String.format("%.2f KB", metadata.originalSizeBytes / 1024.0),
                                valueColor = MaterialTheme.colorScheme.primary
                            )

                            InfoRow(
                                label = "Transfer Mode",
                                value = "Original (No Compression)"
                            )

                            InfoRow(
                                label = "Total Chunks",
                                value = "${metadata.totalChunks}"
                            )

                            if (metadata.fileChecksum.isNotEmpty()) {
                                InfoRow(
                                    label = "Checksum",
                                    value = metadata.fileChecksum.take(16) + "...",
                                    valueStyle = MaterialTheme.typography.bodySmall
                                )

                                InfoRow(
                                    label = "Integrity",
                                    value = "✓ Verified",
                                    valueColor = MaterialTheme.colorScheme.primary
                                )
                            }

                            InfoRow(
                                label = "Timestamp",
                                value = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                    .format(Date(metadata.timestamp))
                            )
                        }
                    }
                }

                // Image preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    state.imageUri?.let { uri ->
                        coil.compose.AsyncImage(
                            model = uri,
                            contentDescription = "Received image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action buttons
                when (state.saveState) {
                    is SaveState.Saving -> {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is SaveState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = (state.saveState as SaveState.Error).message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Button(
                            onClick = {
                                val filename = state.metadata?.filename ?: run {
                                    val extension = getExtensionFromMimeType(state.metadata?.mimeType)
                                    "pixelbeam_${System.currentTimeMillis()}$extension"
                                }
                                saveFileLauncher.launch(filename)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Retry Save",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                ReconstructedImageHolder.clear(context)
                                onDiscard()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Discard",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                val filename = state.metadata?.filename ?: run {
                                    val extension = getExtensionFromMimeType(state.metadata?.mimeType)
                                    "pixelbeam_${System.currentTimeMillis()}$extension"
                                }
                                saveFileLauncher.launch(filename)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Save Image",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                ReconstructedImageHolder.clear(context)
                                onDiscard()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Text(
                                text = "Discard",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    valueStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = valueStyle,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

/**
 * Get file extension from MIME type
 */
private fun getExtensionFromMimeType(mimeType: String?): String {
    return when (mimeType) {
        "image/jpeg", "image/jpg" -> ".jpg"
        "image/png" -> ".png"
        "image/gif" -> ".gif"
        "image/webp" -> ".webp"
        "image/bmp" -> ".bmp"
        "image/heic" -> ".heic"
        "image/heif" -> ".heif"
        else -> ".jpg" // Default to .jpg
    }
}
