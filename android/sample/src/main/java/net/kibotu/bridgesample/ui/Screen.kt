package net.kibotu.bridgesample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.kibotu.bridgesample.WebViewScreen
import net.kibotu.jsbridge.JavaScriptBridge
import net.kibotu.jsbridge.SafeAreaService
import net.kibotu.jsbridge.commands.bottomnavigation.BottomNavigationService
import net.kibotu.jsbridge.commands.topnavigation.TopNavigationConfig
import net.kibotu.jsbridge.commands.topnavigation.TopNavigationService

@ExperimentalMaterial3Api
@Composable
fun Screen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onBridgeReady: (JavaScriptBridge) -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    val topNavConfig by TopNavigationService.config.collectAsState(TopNavigationConfig())
    val isBottomBarVisible by BottomNavigationService.isVisible.collectAsState(true)

    val density = LocalDensity.current

    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> navController.navigate("tab1") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            1 -> navController.navigate("tab2") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                AnimatedVisibility(
                    visible = topNavConfig.isVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + expandVertically() + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .onGloballyPositioned {
                                SafeAreaService.topBarHeightDp = pxToDp(it.size.height, density.density)
                            }
                    ) {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            title = {
                                val titleText = if (topNavConfig.showLogo) {
                                    "My Title"
                                } else {
                                    topNavConfig.title ?: "Bridge Sample"
                                }
                                Text(text = titleText)
                            },
                            navigationIcon = {
                                if (topNavConfig.showUpArrow) {
                                    IconButton(onClick = { onBackPressed() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = onToggleTheme) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = "Toggle theme",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (topNavConfig.showProfileIconWidget) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = "Profile"
                                    )
                                }
                            }
                        )
                        if (topNavConfig.showDivider) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            },
            bottomBar = { }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "tab1",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                composable("tab1") {
                    WebViewScreen(
                        url = "file:///android_asset/index.html",
                        onBridgeReady = { onBridgeReady(it) }
                    )
                }
                composable("tab2") {
                    WebViewScreen(
                        url = "https://kibotu.net/check24/jenkins/safearea/",
                        onBridgeReady = { onBridgeReady(it) }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                NavigationBar(
                    modifier = Modifier.onGloballyPositioned {
                        SafeAreaService.bottomBarHeightDp = pxToDp(it.size.height, density.density)
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets(0)
                ) {
                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        label = { Text("Assets") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = null
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        label = { Text("Portfolio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ),
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Public,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun pxToDp(px: Int, density: Float): Int = (px / density).toInt()
