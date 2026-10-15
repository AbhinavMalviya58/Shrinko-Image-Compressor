package com.cheatpot.shrinko.presentation.selection

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cheatpot.shrinko.data.image.ImageMetadataReader
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.presentation.components.BottomActionBar
import com.cheatpot.shrinko.presentation.components.EmptyState
import com.cheatpot.shrinko.presentation.components.PrimaryButton
import com.cheatpot.shrinko.presentation.components.SecondaryButton
import com.cheatpot.shrinko.presentation.components.SelectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageSelectionScreen(
    initialUris: List<Uri>,
    onBack: () -> Unit,
    onProceedToEditor: (List<ImageInfo>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val metadataReader = remember { ImageMetadataReader(context) }

    var loadedImages by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { newUris ->
        if (newUris.isNotEmpty()) {
            isLoading = true
            // Take persistable URI permission so the content:// URIs remain readable
            // beyond this callback (e.g. when ImageCompressor opens a stream later).
            newUris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            scope.launch {
                val newInfos = withContext(Dispatchers.IO) {
                    newUris.mapNotNull { uri -> metadataReader.readImageInfo(uri).getOrNull() }
                }
                loadedImages = (loadedImages + newInfos).distinctBy { it.uriString }
                isLoading = false
            }
        }
    }

    LaunchedEffect(initialUris) {
        if (initialUris.isNotEmpty()) {
            isLoading = true
            // Persist read permission for initially-supplied URIs too.
            initialUris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            val infos = withContext(Dispatchers.IO) {
                initialUris.mapNotNull { uri -> metadataReader.readImageInfo(uri).getOrNull() }
            }
            loadedImages = infos.distinctBy { it.uriString }
            isLoading = false
        } else {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                        text = "Selected Images",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${loadedImages.size} image(s) chosen",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (loadedImages.isNotEmpty()) {
                IconButton(
                    onClick = { loadedImages = emptyList() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Selection",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (loadedImages.isEmpty()) {
                EmptyState(
                    title = "No Images Selected",
                    message = "Tap below to pick photos from your device gallery.",
                    icon = Icons.Default.AddPhotoAlternate,
                    action = {
                        PrimaryButton(
                            onClick = {
                                pickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text("Pick Photos")
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(loadedImages, key = { it.uriString }) { info ->
                        SelectionCard(
                            imageInfo = info,
                            isSelected = true,
                            onSelect = {},
                            onRemove = {
                                loadedImages = loadedImages.filterNot { it.uriString == info.uriString }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        if (loadedImages.isNotEmpty()) {
            BottomActionBar {
                SecondaryButton(
                    onClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add More")
                }

                PrimaryButton(
                    onClick = { onProceedToEditor(loadedImages) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (loadedImages.size > 1) "Process (${loadedImages.size})" else "Configure")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                }
            }
        }
    }
}
