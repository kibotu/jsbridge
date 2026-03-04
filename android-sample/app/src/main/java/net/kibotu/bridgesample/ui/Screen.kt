package net.kibotu.bridgesample.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.kibotu.bridgesample.WebViewScreen
import net.kibotu.bridgesample.bridge.JavaScriptBridge
import net.kibotu.bridgesample.bridge.commands.bottomnavigation.BottomNavigationService
import net.kibotu.bridgesample.bridge.commands.topnavigation.TopNavigationConfig
import net.kibotu.bridgesample.bridge.commands.topnavigation.TopNavigationService

@ExperimentalMaterial3Api
@Composable
fun Screen(
    onBridgeReady: (JavaScriptBridge) -> Unit,
    onBackPressed: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val navController = rememberNavController()
    val topNavConfig by TopNavigationService.config.collectAsState(TopNavigationConfig())
    val isBottomBarVisible by BottomNavigationService.isVisible.collectAsState(true)

    LaunchedEffect(selectedTabIndex) {
        when (selectedTabIndex) {
            0 -> navController.navigate("tab1") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }

            1 -> navController.navigate("tab2") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0),
            topBar = {
                Column(modifier = Modifier.animateContentSize()) {
                    if (topNavConfig.isVisible) {
                        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                            TopAppBar(
                                title = {
                                    val titleText = if (topNavConfig.showLogo) {
                                        "CHECK24"
                                    } else {
                                        topNavConfig.title ?: "Check-Mate Bridge Sample"
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
                                    if (topNavConfig.showProfileIconWidget) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = "Profile"
                                        )
                                    }
                                }
                            )
                            if (topNavConfig.showDivider) {
                                HorizontalDivider()
                            }
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
                    .animateContentSize()
            ) {
                composable("tab1") {
                    WebViewScreen(
                        url = "file:///android_asset/index.html",
                        onBridgeReady = { onBridgeReady(it) })
                }
                composable("tab2") {
                    WebViewScreen(
                        url = "https://trail.services.kibotu.net",
                        onBridgeReady = { onBridgeReady(it) })
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column {
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    NavigationBar(
                        containerColor = Color.White,
                        windowInsets = WindowInsets(0)
                    ) {
                        NavigationBarItem(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            label = { Text("Assets") },
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
}