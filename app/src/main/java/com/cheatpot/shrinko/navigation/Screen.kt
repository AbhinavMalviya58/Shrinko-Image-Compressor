package com.cheatpot.shrinko.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Selection : Screen("selection")
    // Combined optimizer (compress + resize + convert)
    object Editor : Screen("editor")
    object Batch : Screen("batch")
    object Comparison : Screen("comparison")
    object History : Screen("history")
    // Single-purpose tool screens
    object CompressOnly : Screen("compress_only")
    object ResizeOnly : Screen("resize_only")
    object ConvertOnly : Screen("convert_only")
    object Settings : Screen("settings")
}
