package com.cheatpot.shrinko.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun BeforeAfterComparison(
    originalUri: String,
    compressedUri: String,
    modifier: Modifier = Modifier
) {
    var splitPositionFraction by remember { mutableFloatStateOf(0.5f) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val totalWidth = maxWidth
        val containerWidthPx = with(density) { totalWidth.toPx() }
        val halfHandlePx = with(density) { 20.dp.toPx() }
        val handleCenterYPx = with(density) { 140.dp.toPx() }

        // Compressed Image (Background / Right side)
        AsyncImage(
            model = compressedUri,
            contentDescription = "Compressed Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        // Original Image (Foreground / Left side clipped by width)
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(totalWidth * splitPositionFraction)
                .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
        ) {
            AsyncImage(
                model = originalUri,
                contentDescription = "Original Image",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(totalWidth)
            )
        }

        // Left Label (BEFORE / ORIGINAL)
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Text(
                text = "Original",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Right Label (AFTER / COMPRESSED)
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        ) {
            Text(
                text = "Compressed",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        // Draggable Vertical Divider & Handle
        val dividerOffsetPx = containerWidthPx * splitPositionFraction

        Box(
            modifier = Modifier
                .offset { IntOffset(dividerOffsetPx.roundToInt(), 0) }
                .fillMaxHeight()
                .width(4.dp)
                .background(Color.White)
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (dividerOffsetPx - halfHandlePx).roundToInt(),
                        (handleCenterYPx - halfHandlePx).roundToInt()
                    )
                }
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newFraction = (splitPositionFraction + (dragAmount.x / containerWidthPx)).coerceIn(0.05f, 0.95f)
                        splitPositionFraction = newFraction
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.UnfoldMore,
                contentDescription = "Drag to compare",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = 90f)
            )
        }
    }
}
