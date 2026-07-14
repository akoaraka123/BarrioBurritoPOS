package com.example.barrioburritopos.ui.responsive

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

data class ResponsiveInfo(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isLandscape: Boolean,
    val isTablet: Boolean
) {
    val isPhone: Boolean = !isTablet
}

@Composable
fun rememberResponsiveInfo(): ResponsiveInfo {
    val configuration = LocalConfiguration.current
    val shortestSide = minOf(configuration.screenWidthDp, configuration.screenHeightDp)

    return ResponsiveInfo(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE,
        isTablet = shortestSide >= 600
    )
}
