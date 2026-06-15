package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.ui.components.ConnectoLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeviceOnlineStatus
import com.example.ui.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DevicesScreen(
    viewModel: DeviceViewModel,
    onNavigateToAddDevice: () -> Unit
) {
    val devices by viewModel.allDevices.collectAsState()
    var deviceToDelete by remember { mutableStateOf<Device?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dots")
    val dotPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Your Devices", 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            if (devices.isNotEmpty()) {
                FloatingActionButton(
                    onClick = onNavigateToAddDevice,
                    containerColor = TealAccent,
                    contentColor = DarkBg,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Device", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (devices.isEmpty()) {
                // Empty State Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Logo nodes styling representing connecto brand logo
                    ConnectoLogo(
                        modifier = Modifier
                            .size(130.dp)
                            .padding(bottom = 12.dp),
                        animate = true
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "No devices yet.",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Add your laptop, workstation, or server agent to orchestrate shutdowns and states remotely.",
                        color = TextMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onNavigateToAddDevice,
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = DarkBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Device", color = DarkBg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Saved Devices List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(devices) { device ->
                        val status = viewModel.deviceStatuses[device.id] ?: DeviceOnlineStatus.Unknown
                        val isOnline = status == DeviceOnlineStatus.Online
                        val isChecking = status == DeviceOnlineStatus.Checking
                        val statusColor = if (isOnline) TealAccent else if (isChecking) Color.Yellow else ShutdownRed
                        
                        // Select type emoji
                        val emoji = when (device.type.lowercase()) {
                            "desktop" -> "🖥️"
                            "server" -> "🖳"
                            else -> "💻"
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (device.isActive) CardSurface else CardSurface.copy(alpha = 0.61f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            border = if (device.isActive) BorderStroke(1.5.dp, TealAccent) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { viewModel.selectActiveDevice(device.id) },
                                    onLongClick = { deviceToDelete = device }
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Device Emoji Placeholder
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(Color.White.copy(alpha = 0.05f), CircleShape)
                                    ) {
                                        Text(text = emoji, fontSize = 24.sp)
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = device.name,
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            // Status blinker
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .scale(if (isOnline) dotPulseScale else 1f)
                                                    .background(statusColor, CircleShape)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (isOnline) "Connected" else if (isChecking) "Pinging..." else "Offline",
                                            color = statusColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Text(
                                            text = device.ipAddress,
                                            color = TextMuted,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                if (device.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .background(TealAccent.copy(alpha = 0.15f), CircleShape)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = "ACTIVE", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Select",
                                        tint = TextMuted
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Deletion Modal
    if (deviceToDelete != null) {
        val device = deviceToDelete!!
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text(text = "Remove Device", color = Color.White) },
            text = { Text(text = "Are you sure you want to delete '${device.name}' from Connecto list?", color = TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDevice(device)
                        deviceToDelete = null
                    }
                ) {
                    Text(text = "Delete", color = ShutdownRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) {
                    Text(text = "Cancel", color = Color.White)
                }
            },
            containerColor = CardSurface
        )
    }
}
