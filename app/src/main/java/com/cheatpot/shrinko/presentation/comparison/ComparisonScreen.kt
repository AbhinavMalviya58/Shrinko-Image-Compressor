package com.cheatpot.shrinko.presentation.comparison

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.usecase.SaveImageUseCase
import com.cheatpot.shrinko.presentation.components.BeforeAfterComparison
import com.cheatpot.shrinko.presentation.components.BottomActionBar
import com.cheatpot.shrinko.presentation.components.PrimaryButton
import com.cheatpot.shrinko.presentation.components.SecondaryButton
import kotlinx.coroutines.launch

@Composable
fun ComparisonScreen(
    result: CompressionResult,
    saveImageUseCase: SaveImageUseCase,
    onBack: () -> Unit,
    onEditSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var isSaving by remember { mutableStateOf(false) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Comparison & Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = result.originalInfo.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Draggable Before-After Visual Comparison Slider Container
            BeforeAfterComparison(
                originalUri = result.originalInfo.uriString,
                compressedUri = result.outputFileUri
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Results Metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "File Size Reduction",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Saved ${result.savedPercentage.toInt()}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Original Size",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.formattedOriginalSize,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "→",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Compressed Size",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = result.formattedOutputSize,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail rows
                    DetailRow(
                        label = "Dimensions",
                        value = "${result.originalInfo.width}×${result.originalInfo.height} → ${result.outputWidth}×${result.outputHeight} px"
                    )
                    DetailRow(
                        label = "Format",
                        value = "${result.originalInfo.extension.uppercase()} → ${result.outputFormat.displayName}"
                    )
                    DetailRow(
                        label = "Quality Applied",
                        value = "${result.qualityUsed}%"
                    )
                    DetailRow(
                        label = "Processing Time",
                        value = "${result.processingTimeMs} ms"
                    )

                    if (result.dimensionsReduced) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Dimensions were downscaled to optimize file size.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom Action Bar
        BottomActionBar {
            SecondaryButton(
                onClick = {
                    try {
                        // result.outputFileUri is a file:// URI produced by ImageCompressor.
                        // We MUST convert it to a content:// URI via FileProvider before
                        // passing it to any external Intent — file:// URIs are blocked on
                        // Android 7+ (FileUriExposedException).
                        val outputUri = Uri.parse(result.outputFileUri)
                        val shareUri: Uri = if (outputUri.scheme == "file") {
                            val file = java.io.File(outputUri.path!!)
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else {
                            // Already a content:// URI (e.g. from MediaStore save)
                            outputUri
                        }

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = result.outputFormat.mimeType
                            putExtra(Intent.EXTRA_STREAM, shareUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Share Compressed Image")
                        )
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Cannot share: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }

            PrimaryButton(
                onClick = {
                    if (savedUri != null) {
                        Toast.makeText(context, "Already saved to Pictures/PixelShrink", Toast.LENGTH_SHORT).show()
                        return@PrimaryButton
                    }
                    scope.launch {
                        isSaving = true
                        val saveRes = saveImageUseCase(result)
                        isSaving = false
                        saveRes.onSuccess { uri ->
                            savedUri = uri
                            Toast.makeText(context, "Saved to Pictures/PixelShrink", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (savedUri != null) "Saved ✓" else if (isSaving) "Saving..." else "Save Image")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
