package com.cheatpot.shrinko.presentation.components

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.cheatpot.shrinko.R
import kotlinx.coroutines.delay

@Composable
fun ProcessingIndicatorDialog(
    statusText: String? = null,
    progress: Float? = null,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Set up Coil ImageLoader with GIF support
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    // Dynamic rotating status messages
    val messages = remember(statusText) {
        if (!statusText.isNullOrBlank()) {
            listOf(
                statusText,
                "Working on your image...",
                "Shrinking pixels to perfection...",
                "Optimizing compression & quality...",
                "Preserving crisp visual details...",
                "Almost ready..."
            )
        } else {
            listOf(
                "We're working on it...",
                "Shrinking pixels to perfection...",
                "Optimizing file size...",
                "Preserving crisp details...",
                "Applying smart compression...",
                "Almost ready..."
            )
        }
    }

    var messageIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            messageIndex = (messageIndex + 1) % messages.size
        }
    }

    // Dynamic progress simulation if exact progress is not provided
    var simulatedProgress by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(Unit) {
        if (progress == null) {
            var current = 0.15f
            while (current < 0.94f) {
                delay(120)
                current += if (current < 0.6f) 0.05f else if (current < 0.85f) 0.025f else 0.008f
                simulatedProgress = current.coerceAtMost(0.95f)
            }
        }
    }

    val effectiveProgress = progress ?: simulatedProgress
    val animatedProgress by animateFloatAsState(
        targetValue = effectiveProgress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "dialog_progress"
    )

    // Shimmer effect animation for progress bar
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    Dialog(
        onDismissRequest = { /* Modal, cannot be dismissed while processing */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── COOL GIF ANIMATION CONTAINER ──────────────────────────
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            )
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.loading)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = "Loading Animation",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── ROTATING STATUS TEXT ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = messages[messageIndex],
                        transitionSpec = {
                            (slideInVertically { height -> height / 2 } + fadeIn(tween(300)))
                                .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut(tween(300)))
                        },
                        label = "status_message_animation"
                    ) { targetMsg ->
                        Text(
                            text = targetMsg,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── PERCENTAGE READOUT ────────────────────────────────────
                val pctInt = (animatedProgress * 100).toInt().coerceIn(1, 99)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$pctInt",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── CUSTOM GRADIENT PROGRESS BAR ──────────────────────────
                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val secondaryColor = MaterialTheme.colorScheme.secondary

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                ) {
                    // Filled progress with gradient
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        primaryColor,
                                        tertiaryColor,
                                        secondaryColor
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom micro tagline
                Text(
                    text = "High performance on-device processing",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
