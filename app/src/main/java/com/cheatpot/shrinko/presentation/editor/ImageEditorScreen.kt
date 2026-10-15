package com.cheatpot.shrinko.presentation.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cheatpot.shrinko.domain.model.AutoQualityPreference
import com.cheatpot.shrinko.domain.model.CompressionMode
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.SocialPreset
import com.cheatpot.shrinko.domain.model.formatFileSize
import com.cheatpot.shrinko.presentation.components.BottomActionBar
import com.cheatpot.shrinko.presentation.components.DimensionInput
import com.cheatpot.shrinko.presentation.components.FormatSelector
import com.cheatpot.shrinko.presentation.components.ImagePreviewCard
import com.cheatpot.shrinko.presentation.components.PrimaryButton
import com.cheatpot.shrinko.presentation.components.ProcessingIndicatorDialog
import com.cheatpot.shrinko.presentation.components.QualitySlider
import com.cheatpot.shrinko.presentation.components.SectionHeader
import com.cheatpot.shrinko.presentation.components.SizePresetChip
import com.cheatpot.shrinko.presentation.home.HomePickerMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageEditorScreen(
    imageInfo: ImageInfo,
    viewModel: ImageEditorViewModel,
    initialTargetBytes: Long? = null,
    initialResizePercentage: Int? = null,
    initialSocialPreset: SocialPreset? = null,
    initialOutputFormat: OutputFormat? = null,
    toolMode: HomePickerMode = HomePickerMode.COMBINED,
    onBack: () -> Unit,
    onCompressionSuccess: (CompressionResult) -> Unit
) {
    LaunchedEffect(imageInfo) {
        viewModel.setImageInfo(imageInfo)
        // Pre-set target size from quick-size selection
        if (initialTargetBytes != null && initialTargetBytes > 0) {
            viewModel.setCompressionMode(CompressionMode.TARGET_SIZE)
            viewModel.setTargetSizeBytes(initialTargetBytes)
        }
        if (initialResizePercentage != null) {
            viewModel.setResizeMode(ResizeMode.PERCENTAGE)
            viewModel.setResizePercentage(initialResizePercentage)
        }
        if (initialSocialPreset != null) {
            viewModel.setSocialPreset(initialSocialPreset)
        }
        if (initialOutputFormat != null && initialOutputFormat != imageInfo.originalFormat) {
            viewModel.setOutputFormat(initialOutputFormat)
        } else if (toolMode == HomePickerMode.CONVERT_ONLY) {
            val defaultAlternative = if (imageInfo.originalFormat == OutputFormat.JPEG) OutputFormat.PNG else OutputFormat.JPEG
            viewModel.setOutputFormat(defaultAlternative)
        }
        // Pre-configure op toggles based on tool mode
        when (toolMode) {
            HomePickerMode.COMPRESS_ONLY -> {
                viewModel.setCompressEnabled(true)
                viewModel.setResizeEnabled(false)
                viewModel.setConvertEnabled(false)
            }
            HomePickerMode.RESIZE_ONLY -> {
                viewModel.setCompressEnabled(false)
                viewModel.setResizeEnabled(true)
                viewModel.setConvertEnabled(false)
                if (initialResizePercentage == null && initialSocialPreset == null && viewModel.uiState.value.resizeSettings.mode == ResizeMode.NONE) {
                    viewModel.setResizeMode(ResizeMode.PERCENTAGE)
                    viewModel.setResizePercentage(75)
                }
            }
            HomePickerMode.CONVERT_ONLY -> {
                viewModel.setCompressEnabled(false)
                viewModel.setResizeEnabled(false)
                viewModel.setConvertEnabled(true)
            }
            HomePickerMode.COMBINED -> {
                viewModel.setCompressEnabled(true)
                viewModel.setResizeEnabled(false)
                viewModel.setConvertEnabled(false)
            }
        }
    }

    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { err ->
            android.widget.Toast.makeText(context, err, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    if (state.isProcessing) {
        val statusText = buildString {
            if (state.compressEnabled) append("Compressing")
            if (state.resizeEnabled) {
                if (length > 0) append(" → "); append("Resizing")
            }
            if (state.convertEnabled) {
                if (length > 0) append(" → "); append("Converting")
            }
            if (isEmpty()) append("Processing")
            append("…")
        }
        ProcessingIndicatorDialog(statusText = statusText)
    }

    // Screen title
    val screenTitle = when (toolMode) {
        HomePickerMode.COMPRESS_ONLY -> "Compress Image"
        HomePickerMode.RESIZE_ONLY -> "Resize Image"
        HomePickerMode.CONVERT_ONLY -> "Convert Format"
        HomePickerMode.COMBINED -> "Image Optimizer"
    }

    val isCombined = toolMode == HomePickerMode.COMBINED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── HEADER ────────────────────────────────────────────────────────────
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
                    text = screenTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = imageInfo.name,
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
            ImagePreviewCard(imageInfo = imageInfo)
            Spacer(modifier = Modifier.height(20.dp))

            // ── COMBINED MODE: Op toggles + sections per op ───────────────────
            if (isCombined) {
                Text(
                    text = "Select Operations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Compress section
                OpSection(
                    icon = Icons.Default.Compress,
                    title = "Compression",
                    subtitle = if (state.compressionSettings.mode == CompressionMode.TARGET_SIZE)
                        "Target ${formatFileSize(state.compressionSettings.targetSizeBytes)}"
                    else "Quality ${state.compressionSettings.quality}%",
                    enabled = state.compressEnabled,
                    onToggle = { viewModel.setCompressEnabled(it) }
                ) {
                    CompressionSettingsContent(state = state, viewModel = viewModel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Resize section
                OpSection(
                    icon = Icons.Default.AspectRatio,
                    title = "Resize",
                    subtitle = when (state.resizeSettings.mode) {
                        ResizeMode.NONE -> "Not configured"
                        ResizeMode.PERCENTAGE -> "${state.resizeSettings.percentage}% scale"
                        ResizeMode.CUSTOM_DIMENSIONS -> "${state.resizeSettings.customWidth}×${state.resizeSettings.customHeight} px"
                        ResizeMode.SOCIAL_PRESET -> state.resizeSettings.socialPreset?.displayName ?: "Preset"
                        ResizeMode.AUTO_RESIZE -> "Auto fit (max 1920px)"
                    },
                    enabled = state.resizeEnabled,
                    onToggle = { viewModel.setResizeEnabled(it) }
                ) {
                    ResizeSettingsContent(state = state, imageInfo = imageInfo, viewModel = viewModel)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Format conversion section
                OpSection(
                    icon = Icons.Default.SwapHoriz,
                    title = "Format Conversion",
                    subtitle = "${imageInfo.extension.uppercase()} → ${state.compressionSettings.outputFormat.displayName}",
                    enabled = state.convertEnabled,
                    onToggle = { viewModel.setConvertEnabled(it) }
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FormatSelector(
                                selectedFormat = state.compressionSettings.outputFormat,
                                onFormatSelected = { viewModel.setOutputFormat(it) },
                                excludedFormat = imageInfo.originalFormat
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── PROCESSING SUMMARY ──────────────────────────────────────
                ProcessingSummaryCard(
                    state = state,
                    imageInfo = imageInfo
                )

            } else {
                // ── SINGLE-TOOL MODE ──────────────────────────────────────────
                when (toolMode) {
                    HomePickerMode.COMPRESS_ONLY -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                SectionHeader(title = "Compression Settings")
                                Spacer(modifier = Modifier.height(8.dp))
                                CompressionSettingsContent(state = state, viewModel = viewModel)
                            }
                        }
                    }
                    HomePickerMode.RESIZE_ONLY -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                SectionHeader(title = "Resize Settings")
                                Spacer(modifier = Modifier.height(8.dp))
                                ResizeSettingsContent(
                                    state = state,
                                    imageInfo = imageInfo,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                    HomePickerMode.CONVERT_ONLY -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                SectionHeader(title = "Output Format")
                                Spacer(modifier = Modifier.height(8.dp))
                                FormatSelector(
                                    selectedFormat = state.compressionSettings.outputFormat,
                                    onFormatSelected = { viewModel.setOutputFormat(it) },
                                    excludedFormat = imageInfo.originalFormat
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── BOTTOM ACTION BAR ─────────────────────────────────────────────────
        BottomActionBar {
            val buttonLabel = when (toolMode) {
                HomePickerMode.COMPRESS_ONLY -> "Compress Now"
                HomePickerMode.RESIZE_ONLY -> "Resize Now"
                HomePickerMode.CONVERT_ONLY -> "Convert Now"
                HomePickerMode.COMBINED -> "Process Image"
            }
            val opsCount = listOf(
                state.compressEnabled,
                state.resizeEnabled,
                state.convertEnabled
            ).count { it }

            PrimaryButton(
                onClick = {
                    viewModel.processCompression { result ->
                        onCompressionSuccess(result)
                    }
                },
                enabled = opsCount > 0 || !isCombined
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                if (isCombined && opsCount > 0) {
                    Text(
                        "$buttonLabel ($opsCount op${if (opsCount > 1) "s" else ""})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(buttonLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── OPERATION SECTION CARD ────────────────────────────────────────────────────
@Composable
private fun OpSection(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (enabled) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Expandable content
            AnimatedVisibility(
                visible = enabled,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }
        }
    }
}

// ── PROCESSING SUMMARY CARD ───────────────────────────────────────────────────
@Composable
private fun ProcessingSummaryCard(
    state: ImageEditorUiState,
    imageInfo: ImageInfo
) {
    val opsCount = listOf(state.compressEnabled, state.resizeEnabled, state.convertEnabled).count { it }
    if (opsCount == 0) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Processing Summary",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "$opsCount operation${if (opsCount > 1) "s" else ""} will be applied sequentially",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Step 1: Compression
            if (state.compressEnabled) {
                SummaryStep(
                    step = "1",
                    label = "Compression",
                    detail = when (state.compressionSettings.mode) {
                        CompressionMode.TARGET_SIZE ->
                            "${formatFileSize(imageInfo.sizeBytes)} → Target ${formatFileSize(state.compressionSettings.targetSizeBytes)}"
                        CompressionMode.QUALITY, CompressionMode.CUSTOM ->
                            "Quality ${state.compressionSettings.quality}% applied"
                        CompressionMode.AUTO ->
                            "${state.compressionSettings.autoQualityPreference.displayName} mode"
                    },
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.resizeEnabled || state.convertEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Step 2: Resize
            if (state.resizeEnabled) {
                val stepNum = if (state.compressEnabled) "2" else "1"
                val resizeDetail = when (state.resizeSettings.mode) {
                    ResizeMode.NONE -> "No resize"
                    ResizeMode.PERCENTAGE -> {
                        val w = (imageInfo.width * state.resizeSettings.percentage / 100f).toInt()
                        val h = (imageInfo.height * state.resizeSettings.percentage / 100f).toInt()
                        "${imageInfo.width}×${imageInfo.height} → $w×$h px"
                    }
                    ResizeMode.CUSTOM_DIMENSIONS ->
                        "${imageInfo.width}×${imageInfo.height} → ${state.resizeSettings.customWidth}×${state.resizeSettings.customHeight} px"
                    ResizeMode.SOCIAL_PRESET -> {
                        val p = state.resizeSettings.socialPreset
                        if (p != null) "${p.displayName} (${p.width}×${p.height} px)" else "Social preset"
                    }
                    ResizeMode.AUTO_RESIZE -> "Auto fit, max dimension 1920 px"
                }
                SummaryStep(
                    step = stepNum,
                    label = "Resize",
                    detail = resizeDetail,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (state.convertEnabled) Spacer(modifier = Modifier.height(8.dp))
            }

            // Step 3: Format conversion
            if (state.convertEnabled) {
                val stepNum = when {
                    state.compressEnabled && state.resizeEnabled -> "3"
                    state.compressEnabled || state.resizeEnabled -> "2"
                    else -> "1"
                }
                SummaryStep(
                    step = stepNum,
                    label = "Format Conversion",
                    detail = "${imageInfo.extension.uppercase()} → ${state.compressionSettings.outputFormat.displayName}",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun SummaryStep(step: String, label: String, detail: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ── COMPRESSION SETTINGS ──────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompressionSettingsContent(
    state: ImageEditorUiState,
    viewModel: ImageEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Mode selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            CompressionMode.values().forEach { mode ->
                val isSelected = state.compressionSettings.mode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else Color.Transparent
                        )
                        .clickable { viewModel.setCompressionMode(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (state.compressionSettings.mode) {
            CompressionMode.AUTO -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AutoQualityPreference.values().forEach { pref ->
                        val isSel = state.compressionSettings.autoQualityPreference == pref
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setAutoQualityPreference(pref) },
                            label = { Text(pref.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            CompressionMode.TARGET_SIZE -> {
                SizePresetChip(
                    selectedBytes = state.compressionSettings.targetSizeBytes,
                    onBytesSelected = { viewModel.setTargetSizeBytes(it) }
                )
                Spacer(modifier = Modifier.height(12.dp))
                var customKbInput by remember {
                    mutableStateOf((state.compressionSettings.targetSizeBytes / 1024).toString())
                }
                OutlinedTextField(
                    value = customKbInput,
                    onValueChange = { input ->
                        customKbInput = input
                        val kb = input.toLongOrNull() ?: 0L
                        if (kb > 0) viewModel.setTargetSizeBytes(kb * 1024L)
                    },
                    label = { Text("Custom target (KB)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            CompressionMode.QUALITY, CompressionMode.CUSTOM -> {
                QualitySlider(
                    quality = state.compressionSettings.quality,
                    onQualityChange = { viewModel.setQuality(it) }
                )
            }
        }
    }
}

// ── RESIZE SETTINGS ───────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResizeSettingsContent(
    state: ImageEditorUiState,
    imageInfo: ImageInfo,
    viewModel: ImageEditorViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ResizeMode.values().forEach { mode ->
                val isSel = state.resizeSettings.mode == mode
                FilterChip(
                    selected = isSel,
                    onClick = { viewModel.setResizeMode(mode) },
                    label = { Text(mode.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (state.resizeSettings.mode) {
            ResizeMode.NONE -> {
                Text(
                    text = "Original dimensions ${imageInfo.width}×${imageInfo.height} px will be preserved.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ResizeMode.PERCENTAGE -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(25, 50, 60, 75, 80, 90).forEach { pct ->
                        val isSel = state.resizeSettings.percentage == pct
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setResizePercentage(pct) },
                            label = { Text("$pct%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                val w = (imageInfo.width * state.resizeSettings.percentage / 100f).toInt()
                val h = (imageInfo.height * state.resizeSettings.percentage / 100f).toInt()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "${imageInfo.width}×${imageInfo.height} → $w×$h px",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            ResizeMode.CUSTOM_DIMENSIONS -> {
                DimensionInput(
                    width = state.resizeSettings.customWidth,
                    height = state.resizeSettings.customHeight,
                    lockAspectRatio = state.resizeSettings.lockAspectRatio,
                    onWidthChange = { viewModel.setCustomWidth(it) },
                    onHeightChange = { viewModel.setCustomHeight(it) },
                    onLockToggle = { viewModel.toggleLockAspectRatio() },
                    onReset = { viewModel.resetDimensions() }
                )
            }
            ResizeMode.SOCIAL_PRESET -> {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SocialPreset.values().forEach { preset ->
                        val isSel = state.resizeSettings.socialPreset == preset
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setSocialPreset(preset) },
                            label = { Text("${preset.displayName} (${preset.width}×${preset.height})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
            ResizeMode.AUTO_RESIZE -> {
                Text(
                    text = "Auto Fit caps the maximum dimension at 1920 px while preserving aspect ratio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
