package com.cheatpot.shrinko.navigation

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cheatpot.shrinko.domain.model.CompressionMode
import com.cheatpot.shrinko.domain.model.CompressionResult
import com.cheatpot.shrinko.domain.model.CompressionSettings
import com.cheatpot.shrinko.domain.model.ImageInfo
import com.cheatpot.shrinko.domain.model.OutputFormat
import com.cheatpot.shrinko.domain.model.ResizeMode
import com.cheatpot.shrinko.domain.model.ResizeSettings
import com.cheatpot.shrinko.domain.model.SocialPreset
import com.cheatpot.shrinko.domain.repository.ImageRepository
import com.cheatpot.shrinko.domain.usecase.CompressImageUseCase
import com.cheatpot.shrinko.domain.usecase.SaveImageUseCase
import com.cheatpot.shrinko.presentation.batch.BatchProcessingScreen
import com.cheatpot.shrinko.presentation.batch.BatchViewModel
import com.cheatpot.shrinko.presentation.comparison.ComparisonScreen
import com.cheatpot.shrinko.presentation.editor.ImageEditorScreen
import com.cheatpot.shrinko.presentation.editor.ImageEditorViewModel
import com.cheatpot.shrinko.presentation.history.HistoryScreen
import com.cheatpot.shrinko.presentation.history.HistoryViewModel
import com.cheatpot.shrinko.presentation.home.HomePickerMode
import com.cheatpot.shrinko.presentation.home.HomeScreen
import com.cheatpot.shrinko.presentation.home.HomeViewModel
import com.cheatpot.shrinko.presentation.selection.ImageSelectionScreen
import com.cheatpot.shrinko.presentation.settings.SettingsScreen
import com.cheatpot.shrinko.presentation.settings.SettingsViewModel
import com.cheatpot.shrinko.presentation.theme.PixelShrinkTheme

@Composable
fun AppNavigation(
    imageRepository: ImageRepository
) {
    val navController = rememberNavController()

    // ViewModels
    val homeViewModel = remember { HomeViewModel(imageRepository) }
    val compressImageUseCase = remember { CompressImageUseCase(imageRepository) }
    val saveImageUseCase = remember { SaveImageUseCase(imageRepository) }
    val editorViewModel = remember { ImageEditorViewModel(compressImageUseCase) }
    val batchViewModel = remember { BatchViewModel(imageRepository) }
    val historyViewModel = remember { HistoryViewModel(imageRepository) }
    val settingsViewModel = remember { SettingsViewModel(imageRepository) }

    val appSettings by settingsViewModel.appSettings.collectAsState()

    // Navigation state for passing objects between screens
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedImageInfos by remember { mutableStateOf<List<ImageInfo>>(emptyList()) }
    var activeComparisonResult by remember { mutableStateOf<CompressionResult?>(null) }
    
    // Quick presets passed from home to editor
    var quickTargetBytes by remember { mutableStateOf<Long?>(null) }
    var quickResizePercentage by remember { mutableStateOf<Int?>(null) }
    var quickSocialPreset by remember { mutableStateOf<SocialPreset?>(null) }
    var quickOutputFormat by remember { mutableStateOf<OutputFormat?>(null) }

    // Which mode each selection picker was launched with
    var pickerMode by remember { mutableStateOf(HomePickerMode.COMBINED) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Bottom bar only on root destinations (Home, History)
    val showBottomBar = currentRoute in listOf(Screen.Home.route, Screen.History.route)

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    PixelShrinkTheme(themeMode = appSettings.themeMode) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    CompactBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route -> navigateTopLevel(route) }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // ── HOME ─────────────────────────────────────────────────────
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onLaunchSelection = { uris, mode, targetBytes, resizePct, socialPreset, outFormat ->
                            selectedUris = uris
                            pickerMode = mode
                            quickTargetBytes = targetBytes
                            quickResizePercentage = resizePct
                            quickSocialPreset = socialPreset
                            quickOutputFormat = outFormat
                            navController.navigate(Screen.Selection.route)
                        },
                        onOpenSettings = { navController.navigate(Screen.Settings.route) },
                        onOpenHistory = { navigateTopLevel(Screen.History.route) }
                    )
                }

                // ── SELECTION ─────────────────────────────────────────────────
                composable(Screen.Selection.route) {
                    ImageSelectionScreen(
                        initialUris = selectedUris,
                        onBack = { navController.popBackStack() },
                        onProceedToEditor = { infos ->
                            selectedImageInfos = infos
                            if (infos.size == 1) {
                                // Route to the dedicated editor screen based on mode
                                when (pickerMode) {
                                    HomePickerMode.COMPRESS_ONLY -> navController.navigate(Screen.CompressOnly.route)
                                    HomePickerMode.RESIZE_ONLY -> navController.navigate(Screen.ResizeOnly.route)
                                    HomePickerMode.CONVERT_ONLY -> navController.navigate(Screen.ConvertOnly.route)
                                    HomePickerMode.COMBINED -> navController.navigate(Screen.Editor.route)
                                }
                            } else {
                                navController.navigate(Screen.Batch.route)
                            }
                        }
                    )
                }

                // ── COMBINED EDITOR (all 3 ops together) ───────────────────────
                composable(Screen.Editor.route) {
                    val info = selectedImageInfos.firstOrNull()
                    if (info != null) {
                        ImageEditorScreen(
                            imageInfo = info,
                            viewModel = editorViewModel,
                            initialTargetBytes = quickTargetBytes,
                            initialResizePercentage = quickResizePercentage,
                            initialSocialPreset = quickSocialPreset,
                            initialOutputFormat = quickOutputFormat,
                            toolMode = HomePickerMode.COMBINED,
                            onBack = { navController.popBackStack() },
                            onCompressionSuccess = { result ->
                                quickTargetBytes = null
                                quickResizePercentage = null
                                quickSocialPreset = null
                                quickOutputFormat = null
                                activeComparisonResult = result
                                navController.navigate(Screen.Comparison.route)
                            }
                        )
                    }
                }

                // ── COMPRESS ONLY ─────────────────────────────────────────────
                composable(Screen.CompressOnly.route) {
                    val info = selectedImageInfos.firstOrNull()
                    if (info != null) {
                        ImageEditorScreen(
                            imageInfo = info,
                            viewModel = editorViewModel,
                            initialTargetBytes = quickTargetBytes,
                            toolMode = HomePickerMode.COMPRESS_ONLY,
                            onBack = { navController.popBackStack() },
                            onCompressionSuccess = { result ->
                                quickTargetBytes = null
                                activeComparisonResult = result
                                navController.navigate(Screen.Comparison.route)
                            }
                        )
                    }
                }

                // ── RESIZE ONLY ───────────────────────────────────────────────
                composable(Screen.ResizeOnly.route) {
                    val info = selectedImageInfos.firstOrNull()
                    if (info != null) {
                        ImageEditorScreen(
                            imageInfo = info,
                            viewModel = editorViewModel,
                            initialResizePercentage = quickResizePercentage,
                            initialSocialPreset = quickSocialPreset,
                            toolMode = HomePickerMode.RESIZE_ONLY,
                            onBack = { navController.popBackStack() },
                            onCompressionSuccess = { result ->
                                quickResizePercentage = null
                                quickSocialPreset = null
                                activeComparisonResult = result
                                navController.navigate(Screen.Comparison.route)
                            }
                        )
                    }
                }

                // ── CONVERT ONLY ──────────────────────────────────────────────
                composable(Screen.ConvertOnly.route) {
                    val info = selectedImageInfos.firstOrNull()
                    if (info != null) {
                        ImageEditorScreen(
                            imageInfo = info,
                            viewModel = editorViewModel,
                            initialOutputFormat = quickOutputFormat,
                            toolMode = HomePickerMode.CONVERT_ONLY,
                            onBack = { navController.popBackStack() },
                            onCompressionSuccess = { result ->
                                quickOutputFormat = null
                                activeComparisonResult = result
                                navController.navigate(Screen.Comparison.route)
                            }
                        )
                    }
                }

                // ── BATCH PROCESSING ──────────────────────────────────────────
                composable(Screen.Batch.route) {
                    BatchProcessingScreen(
                        images = selectedImageInfos,
                        compressionSettings = CompressionSettings(
                            mode = if (quickTargetBytes != null) CompressionMode.TARGET_SIZE else CompressionMode.AUTO,
                            targetSizeBytes = quickTargetBytes ?: (200 * 1024L),
                            outputFormat = quickOutputFormat ?: OutputFormat.JPEG
                        ),
                        resizeSettings = ResizeSettings(
                            mode = if (quickResizePercentage != null) ResizeMode.PERCENTAGE else ResizeMode.NONE,
                            percentage = quickResizePercentage ?: 100
                        ),
                        viewModel = batchViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                // ── COMPARISON ────────────────────────────────────────────────
                composable(Screen.Comparison.route) {
                    val compResult = activeComparisonResult
                    if (compResult != null) {
                        ComparisonScreen(
                            result = compResult,
                            saveImageUseCase = saveImageUseCase,
                            onBack = { navController.popBackStack() },
                            onEditSettings = { navController.navigate(Screen.Editor.route) }
                        )
                    }
                }

                // ── HISTORY ───────────────────────────────────────────────────
                composable(Screen.History.route) {
                    HistoryScreen(
                        viewModel = historyViewModel,
                        onBack = { navController.popBackStack() },
                        onHistoryItemClick = { item ->
                            activeComparisonResult = CompressionResult(
                                id = item.id,
                                originalInfo = ImageInfo(
                                    uriString = item.originalUriString,
                                    name = item.filename,
                                    sizeBytes = item.originalSizeBytes,
                                    width = item.outputWidth,
                                    height = item.outputHeight,
                                    mimeType = item.outputFormat.mimeType
                                ),
                                outputFileUri = item.outputUriString,
                                outputSizeBytes = item.outputSizeBytes,
                                outputWidth = item.outputWidth,
                                outputHeight = item.outputHeight,
                                outputFormat = item.outputFormat,
                                qualityUsed = 75,
                                iterationsCount = 1
                            )
                            navController.navigate(Screen.Comparison.route)
                        }
                    )
                }

                // ── SETTINGS ──────────────────────────────────────────────────
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    val isHistory = currentRoute == Screen.History.route
    val targetIndex = if (isHistory) 1 else 0

    val animatedIndex by animateFloatAsState(
        targetValue = targetIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "liquid_slider"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 6.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .padding(horizontal = 24.dp, vertical = 4.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / 2

            // Liquid Sliding Pill Indicator
            Box(
                modifier = Modifier
                    .width(tabWidth)
                    .fillMaxHeight()
                    .offset(x = tabWidth * animatedIndex)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            )
                        )
                    )
            )

            // Tabs Row
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home Tab
                val isHomeSelected = !isHistory
                val homeScale by animateFloatAsState(
                    targetValue = if (isHomeSelected) 1.06f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "home_scale"
                )
                val homeColor by animateColorAsState(
                    targetValue = if (isHomeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "home_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isHomeSelected) onNavigate(Screen.Home.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = homeScale
                            scaleY = homeScale
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = homeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Home",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isHomeSelected) FontWeight.Bold else FontWeight.Medium,
                            color = homeColor,
                            fontSize = 13.sp
                        )
                    }
                }

                // History Tab
                val isHistorySelected = isHistory
                val historyScale by animateFloatAsState(
                    targetValue = if (isHistorySelected) 1.06f else 1.0f,
                    animationSpec = spring(dampingRatio = 0.7f),
                    label = "history_scale"
                )
                val historyColor by animateColorAsState(
                    targetValue = if (isHistorySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "history_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isHistorySelected) onNavigate(Screen.History.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.graphicsLayer {
                            scaleX = historyScale
                            scaleY = historyScale
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "History",
                            tint = historyColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "History",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isHistorySelected) FontWeight.Bold else FontWeight.Medium,
                            color = historyColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
