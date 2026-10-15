package com.cheatpot.shrinko.presentation.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cheatpot.shrinko.R
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.SocialPreset
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class HomePickerMode {
    COMBINED,       // Combined optimizer (all ops)
    COMPRESS_ONLY,
    RESIZE_ONLY,
    CONVERT_ONLY
}

private data class QuickCompressOption(val targetBytes: Long, val label: String)
private data class QuickResizeScaleOption(val percentage: Int, val label: String)
private data class QuickResizeSocialOption(val preset: SocialPreset, val label: String)
private data class QuickConvertOption(val format: OutputFormat, val label: String)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLaunchSelection: (
        uris: List<Uri>,
        mode: HomePickerMode,
        targetBytes: Long?,
        resizePercentage: Int?,
        socialPreset: SocialPreset?,
        outputFormat: OutputFormat?
    ) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit
) {
    var pendingMode by remember { mutableStateOf(HomePickerMode.COMBINED) }
    var pendingTargetBytes by remember { mutableStateOf<Long?>(null) }
    var pendingResizePercentage by remember { mutableStateOf<Int?>(null) }
    var pendingSocialPreset by remember { mutableStateOf<SocialPreset?>(null) }
    var pendingOutputFormat by remember { mutableStateOf<OutputFormat?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onLaunchSelection(
                uris,
                pendingMode,
                pendingTargetBytes,
                pendingResizePercentage,
                pendingSocialPreset,
                pendingOutputFormat
            )
        }
    }

    fun pickImages(
        mode: HomePickerMode,
        targetBytes: Long? = null,
        resizePercentage: Int? = null,
        socialPreset: SocialPreset? = null,
        outputFormat: OutputFormat? = null
    ) {
        pendingMode = mode
        pendingTargetBytes = targetBytes
        pendingResizePercentage = resizePercentage
        pendingSocialPreset = socialPreset
        pendingOutputFormat = outputFormat
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    // Auto-scrolling Carousel State (3 pages: Compress, Resize, Convert)
    val pagerState = rememberPagerState(pageCount = { 3 })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(2200)
            if (!pagerState.isScrollInProgress) {
                val nextPage = (pagerState.currentPage + 1) % 3
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // ── NON-SCROLLABLE MAIN CONTAINER (FILLS SCREEN PROPORTIONALLY) ───────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── 1. FIXED TOP BAR (SETTINGS ONLY) ──────────────────────────────────
        Surface(
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.shrinko_logo),
                            contentDescription = "Shrinko Logo",
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Shrinko",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 19.sp
                        )
                        Text(
                            text = "Compress · Resize · Convert",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                // Top Bar has ONLY Settings option
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── 2. BODY CONTENT (PROPORTIONALLY BALANCED ACROSS SCREEN) ─────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── A. HERO CARD WITH FLOATING/MOVING GLOWING ORBS ────────────────
            AnimatedHeroCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onSelectClick = { pickImages(HomePickerMode.COMBINED) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── B. AUTO-MOVING QUICK PRESETS CAROUSEL ─────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Presets",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp
                    )

                    // Carousel Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 18.dp else 6.dp, 6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> QuickCompressCarouselCard(
                            onOptionSelected = { bytes ->
                                pickImages(HomePickerMode.COMPRESS_ONLY, targetBytes = bytes)
                            }
                        )
                        1 -> QuickResizeCarouselCard(
                            onScaleSelected = { pct ->
                                pickImages(HomePickerMode.RESIZE_ONLY, resizePercentage = pct)
                            },
                            onPresetSelected = { preset ->
                                pickImages(HomePickerMode.RESIZE_ONLY, socialPreset = preset)
                            }
                        )
                        2 -> QuickConvertCarouselCard(
                            onFormatSelected = { format ->
                                pickImages(HomePickerMode.CONVERT_ONLY, outputFormat = format)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── C. THREE MAIN TOOL CARDS (EQUAL SQUARE SHAPE 1:1) ─────────────
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Main Tools",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Tool 1: Compress Image (Equal Square)
                    ToolCardItem(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        painter = painterResource(R.drawable.compressed),
                        title = "Compress",
                        subtitle = "Reduce Size",
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = { pickImages(HomePickerMode.COMPRESS_ONLY) }
                    )

                    // Tool 2: Resize Image (Equal Square)
                    ToolCardItem(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        painter = painterResource(R.drawable.resize),
                        title = "Resize",
                        subtitle = "Scale & Fit",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        onClick = { pickImages(HomePickerMode.RESIZE_ONLY) }
                    )

                    // Tool 3: Convert Format (Equal Square)
                    ToolCardItem(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        painter = painterResource(R.drawable.convert),
                        title = "Convert",
                        subtitle = "JPG · PNG",
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = { pickImages(HomePickerMode.CONVERT_ONLY) }
                    )
                }
            }
        }
    }
}

// ── ANIMATED HERO CARD WITH DYNAMICALLY MOVING FLOATING ORBS ──────────────────

@Composable
private fun AnimatedHeroCard(
    modifier: Modifier = Modifier,
    onSelectClick: () -> Unit
) {
    // 1. Continuous smooth gradient sweep animation
    val infiniteTransition = rememberInfiniteTransition(label = "hero_anim")

    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_shift"
    )

    // 2. Dynamic multi-phase continuous motion for the floating orbs
    val orbPhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb1_motion"
    )

    val orbPhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb2_motion"
    )

    val orbPhase3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb3_motion"
    )

    val orbPhase4 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb4_motion"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val animatedBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5),
            Color(0xFF6366F1),
            Color(0xFF3B82F6),
            Color(0xFF2563EB)
        ),
        start = Offset(gradientShift * 800f, 0f),
        end = Offset(gradientShift * 800f + 800f, 800f)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = primaryColor.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(animatedBrush)
        ) {
            // Dynamic moving offsets for each floating orb inside the card
            val orb1OffsetX = (cos(orbPhase1) * 55 - 10).dp
            val orb1OffsetY = (sin(orbPhase1) * 35 + 5).dp

            val orb2OffsetX = (sin(orbPhase2) * 60 + 15).dp
            val orb2OffsetY = (cos(orbPhase2) * 30 - 10).dp

            val orb3OffsetX = (cos(orbPhase3 * 1.3f) * 45).dp
            val orb3OffsetY = (sin(orbPhase3 * 1.1f) * 35).dp

            val orb4OffsetX = (sin(orbPhase4 * 1.4f) * 50 - 20).dp
            val orb4OffsetY = (cos(orbPhase4 * 1.2f) * 25).dp

            // Moving Glowing Orb 1 (Top-Right / Cyan-Blue Glass Orb)
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = orb1OffsetX, y = orb1OffsetY)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color(0xFF67E8F9).copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.28f), CircleShape)
            )

            // Moving Glowing Orb 2 (Bottom-Left / Violet Glass Orb)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = orb2OffsetX, y = orb2OffsetY)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFA78BFA).copy(alpha = 0.25f),
                                Color(0xFF818CF8).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            )

            // Moving Glowing Orb 3 (Center / Luminous Glass Bubble)
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.Center)
                    .offset(x = orb3OffsetX, y = orb3OffsetY)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color(0xFFC7D2FE).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.30f), CircleShape)
            )

            // Moving Glowing Orb 4 (Top-Left / Pink Bubble)
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.TopStart)
                    .offset(x = orb4OffsetX, y = orb4OffsetY)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFFF472B6).copy(alpha = 0.25f),
                                Color(0xFFFB7185).copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            )

            // Content on Top of Moving Orbs
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.24f)
                    ) {
                        Text(
                            text = "✨  All-in-One Optimizer",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Make Your Image\nLighter.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 28.sp,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Compress, resize & convert in one unified flow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 12.5.sp
                    )
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectClick() },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Select Images",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── COMPACT HORIZONTAL TOOL CARD (ROW OF 3 EQUAL SQUARES 1:1) ─────────────────

@Composable
private fun ToolCardItem(
    modifier: Modifier = Modifier,
    painter: Painter,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.20f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── COMPACT CAROUSEL CONTAINERS & PRESET PAGES ────────────────────────────────

@Composable
private fun QuickCompressCarouselCard(
    modifier: Modifier = Modifier,
    onOptionSelected: (Long) -> Unit
) {
    val compressPresets = listOf(
        QuickCompressOption(50 * 1024L, "50 KB"),
        QuickCompressOption(100 * 1024L, "100 KB"),
        QuickCompressOption(200 * 1024L, "200 KB"),
        QuickCompressOption(300 * 1024L, "300 KB"),
        QuickCompressOption(500 * 1024L, "500 KB"),
        QuickCompressOption(1024 * 1024L, "1 MB"),
        QuickCompressOption(2 * 1024 * 1024L, "2 MB")
    )

    CarouselContainer(
        title = "Compress Target",
        icon = Icons.Default.Compress,
        iconTint = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            compressPresets.forEach { opt ->
                FilterChip(
                    selected = false,
                    onClick = { onOptionSelected(opt.targetBytes) },
                    label = { Text(text = opt.label, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickResizeCarouselCard(
    modifier: Modifier = Modifier,
    onScaleSelected: (Int) -> Unit,
    onPresetSelected: (SocialPreset) -> Unit
) {
    val resizePercentages = listOf(
        QuickResizeScaleOption(25, "25% Scale"),
        QuickResizeScaleOption(50, "50% Scale"),
        QuickResizeScaleOption(75, "75% Scale")
    )

    val resizeSocials = listOf(
        QuickResizeSocialOption(SocialPreset.INSTAGRAM_POST, "1:1 Square"),
        QuickResizeSocialOption(SocialPreset.STANDARD_HD, "16:9 HD"),
        QuickResizeSocialOption(SocialPreset.INSTAGRAM_STORY, "9:16 Story"),
        QuickResizeSocialOption(SocialPreset.WHATSAPP_SHARE, "WhatsApp")
    )

    CarouselContainer(
        title = "Resize Dimensions",
        icon = Icons.Default.AspectRatio,
        iconTint = MaterialTheme.colorScheme.secondary,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            resizePercentages.forEach { opt ->
                FilterChip(
                    selected = false,
                    onClick = { onScaleSelected(opt.percentage) },
                    label = { Text(text = opt.label, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                )
            }

            resizeSocials.forEach { opt ->
                FilterChip(
                    selected = false,
                    onClick = { onPresetSelected(opt.preset) },
                    label = { Text(text = opt.label, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickConvertCarouselCard(
    modifier: Modifier = Modifier,
    onFormatSelected: (OutputFormat) -> Unit
) {
    val convertPresets = listOf(
        QuickConvertOption(OutputFormat.JPEG, "to JPG / JPEG"),
        QuickConvertOption(OutputFormat.PNG, "to PNG (Lossless)"),
        QuickConvertOption(OutputFormat.WEBP, "to WebP (Small & Modern)")
    )

    CarouselContainer(
        title = "Format Conversion",
        icon = Icons.Default.SwapHoriz,
        iconTint = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            convertPresets.forEach { opt ->
                FilterChip(
                    selected = false,
                    onClick = { onFormatSelected(opt.format) },
                    label = { Text(text = opt.label, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(34.dp)
                )
            }
        }
    }
}

@Composable
private fun CarouselContainer(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.5.sp
                )
            }

            content()
        }
    }
}
