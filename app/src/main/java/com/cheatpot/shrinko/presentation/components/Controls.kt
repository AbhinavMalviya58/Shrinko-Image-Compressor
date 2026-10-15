package com.cheatpot.shrinko.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cheatpot.shrinko.domain.model.OutputFormat

@Composable
fun QualitySlider(
    quality: Int,
    onQualityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Compression Quality",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$quality%",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = quality.toFloat(),
            onValueChange = { onQualityChange(it.toInt().coerceIn(1, 100)) },
            valueRange = 1f..100f,
            steps = 98,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val presets = listOf(25 to "25% Low", 50 to "50% Bal", 75 to "75% High", 90 to "90% Max")
            presets.forEach { (pct, label) ->
                val isSelected = quality == pct
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onQualityChange(pct) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun FormatSelector(
    selectedFormat: OutputFormat,
    onFormatSelected: (OutputFormat) -> Unit,
    modifier: Modifier = Modifier,
    excludedFormat: OutputFormat? = null,
    showTitle: Boolean = false
) {
    val displayFormats = remember(excludedFormat) {
        OutputFormat.entries.filter { it != excludedFormat }
    }

    // Auto-select valid format if current selection is excluded
    LaunchedEffect(displayFormats, selectedFormat) {
        if (selectedFormat == excludedFormat && displayFormats.isNotEmpty()) {
            onFormatSelected(displayFormats.first())
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showTitle) {
            Text(
                text = "Output Format",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            displayFormats.forEach { fmt ->
                val isSelected = selectedFormat == fmt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { onFormatSelected(fmt) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fmt.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val desc = when (selectedFormat) {
            OutputFormat.JPEG -> "Best for photographs and maximum file size reduction."
            OutputFormat.PNG -> "Lossless compression with transparency support. Larger file size."
            OutputFormat.WEBP -> "Modern efficient format supporting lossy, lossless, and transparency."
        }

        Text(
            text = desc,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SizePresetChip(
    selectedBytes: Long,
    onBytesSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        100 * 1024L to "100 KB",
        200 * 1024L to "200 KB",
        300 * 1024L to "300 KB",
        500 * 1024L to "500 KB",
        1024 * 1024L to "1 MB",
        2 * 1024 * 1024L to "2 MB",
        5 * 1024 * 1024L to "5 MB"
    )

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { (bytes, label) ->
            val isSelected = selectedBytes == bytes
            FilterChip(
                selected = isSelected,
                onClick = { onBytesSelected(bytes) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun DimensionInput(
    width: Int,
    height: Int,
    lockAspectRatio: Boolean,
    onWidthChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onLockToggle: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target Dimensions (px)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLockToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (lockAspectRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock Aspect Ratio",
                        tint = if (lockAspectRatio) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Dimensions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = if (width > 0) width.toString() else "",
                onValueChange = { val num = it.toIntOrNull() ?: 0; onWidthChange(num) },
                label = { Text("Width (px)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "×",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedTextField(
                value = if (height > 0) height.toString() else "",
                onValueChange = { val num = it.toIntOrNull() ?: 0; onHeightChange(num) },
                label = { Text("Height (px)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                singleLine = true
            )
        }
    }
}
