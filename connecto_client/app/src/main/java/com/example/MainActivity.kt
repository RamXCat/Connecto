package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.TopToastMessage
import com.example.ui.screens.*
import com.example.ui.theme.DarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.DeviceViewModel

enum class ScreenState {
    Splash,
    PinLock,
    Home,
    Devices,
    AddDevice,
    Settings
}

class MainActivity : ComponentActivity() {
    private val viewModel: DeviceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf(ScreenState.Splash) }
                var lastTabScreen by remember { mutableStateOf(ScreenState.Home) } // Fallback memory
                
                // Toast notifications handling
                var currentToast by remember { mutableStateOf("") }
                
                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collect { msg ->
                        currentToast = msg
                    }
                }

                // Handle system back press
                BackHandler(enabled = currentScreen != ScreenState.Home && currentScreen != ScreenState.Splash && currentScreen != ScreenState.PinLock) {
                    when (currentScreen) {
                        ScreenState.AddDevice -> currentScreen = ScreenState.Devices
                        else -> currentScreen = ScreenState.Home
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = DarkBg,
                        bottomBar = {
                            if (currentScreen == ScreenState.Home || 
                                currentScreen == ScreenState.Devices || 
                                currentScreen == ScreenState.Settings) {
                                
                                StitchBottomNavigation(
                                    currentScreen = currentScreen,
                                    onTabSelected = { screen ->
                                        currentScreen = screen
                                        lastTabScreen = screen
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom = if (currentScreen == ScreenState.Home || 
                                                 currentScreen == ScreenState.Devices || 
                                                 currentScreen == ScreenState.Settings) {
                                        innerPadding.calculateBottomPadding()
                                    } else {
                                        0.dp
                                    }
                                )
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                },
                                label = "screen_transitions"
                            ) { targetScreen ->
                                when (targetScreen) {
                                    ScreenState.Splash -> {
                                        SplashScreen(
                                            onSplashComplete = {
                                                if (viewModel.settings.isPinLockEnabled) {
                                                    currentScreen = ScreenState.PinLock
                                                } else {
                                                    currentScreen = ScreenState.Home
                                                }
                                            }
                                        )
                                    }

                                    ScreenState.PinLock -> {
                                        PinLockScreen(
                                            correctPin = viewModel.settings.pinCode,
                                            onSuccess = {
                                                currentScreen = ScreenState.Home
                                            }
                                        )
                                    }

                                    ScreenState.Home -> {
                                        val activeByDb by viewModel.activeDevice.collectAsState()
                                        HomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToDevices = { currentScreen = ScreenState.Devices },
                                            activeDevice = activeByDb
                                        )
                                    }

                                    ScreenState.Devices -> {
                                        DevicesScreen(
                                            viewModel = viewModel,
                                            onNavigateToAddDevice = { currentScreen = ScreenState.AddDevice }
                                        )
                                    }

                                    ScreenState.AddDevice -> {
                                        AddDeviceScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { currentScreen = ScreenState.Devices }
                                        )
                                    }

                                    ScreenState.Settings -> {
                                        SettingsScreen(
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Toast Notification Banner
                    if (currentToast.isNotEmpty()) {
                        TopToastMessage(
                            message = currentToast,
                            onDismiss = { currentToast = "" }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StitchBottomNavigation(
    currentScreen: ScreenState,
    onTabSelected: (ScreenState) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F1C2C), // Dynamic glass-panel surface container low
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = currentScreen == ScreenState.Home,
            onClick = { onTabSelected(ScreenState.Home) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBg,
                selectedTextColor = TealAccent,
                indicatorColor = TealAccent,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.Devices,
            onClick = { onTabSelected(ScreenState.Devices) },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeveloperBoard,
                    contentDescription = "Devices"
                )
            },
            label = { Text("Devices", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBg,
                selectedTextColor = TealAccent,
                indicatorColor = TealAccent,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )

        NavigationBarItem(
            selected = currentScreen == ScreenState.Settings,
            onClick = { onTabSelected(ScreenState.Settings) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            },
            label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkBg,
                selectedTextColor = TealAccent,
                indicatorColor = TealAccent,
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }
}
