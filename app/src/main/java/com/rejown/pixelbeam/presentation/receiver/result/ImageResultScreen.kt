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
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
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
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No Image Data",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onDiscard) {
                        Text("Go Back")
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
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Transfer Complete",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Filename:")
                                Text(
                                    text = metadata.filename,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Original Size:")
                                Text(
                                    text = String.format("%.2f KB", metadata.originalSizeBytes / 1024.0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Received Size:")
                                Text(
                                    text = String.format("%.2f KB", metadata.compressedSizeBytes / 1024.0),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Chunks:")
                                Text(
                                    text = "${metadata.totalChunks}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Timestamp:")
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                        .format(Date(metadata.timestamp)),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Image preview
                Text(
                    text = "Received Image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        Text(
                            text = (state.saveState as SaveState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                val filename = state.metadata?.filename ?: "pixelbeam_${System.currentTimeMillis()}.jpg"
                                saveFileLauncher.launch(filename)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Save")
                        }
                        OutlinedButton(
                            onClick = {
                                ReconstructedImageHolder.clear(context)
                                onDiscard()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Discard")
                        }
                    }

                    else -> {
                        Button(
                            onClick = {
                                val filename = state.metadata?.filename ?: "pixelbeam_${System.currentTimeMillis()}.jpg"
                                saveFileLauncher.launch(filename)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Image")
                        }

                        OutlinedButton(
                            onClick = {
                                ReconstructedImageHolder.clear(context)
                                onDiscard()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Discard")
                        }
                    }
                }
            }
        }
    }
}
