package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Device
import com.example.ui.components.ConnectoLogo
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeviceOnlineStatus
import com.example.ui.viewmodel.DeviceViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: DeviceViewModel,
    onNavigateToDevices: () -> Unit,
    activeDevice: Device?
) {
    var showConfirmModal by remember { mutableStateOf(false) }
    var selectedActionState by remember { mutableStateOf("shutdown") } // "shutdown", "sleep", "restart", "lock"
    
    // Compute current greeting based on local time
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Interactive button scaling animation on tap
    var isPowerButtonPressed by remember { mutableStateOf(false) }
    val powerButtonScale by animateFloatAsState(
        targetValue = if (isPowerButtonPressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "power_scale"
    )

    // Breathing pulse ring animation for online status dot
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_dot_pulse"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConnectoLogo(
                            modifier = Modifier.size(32.dp),
                            animate = false
                        )
                        Text(
                            text = "CONNECTO", 
                            color = Color.White, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg),
                navigationIcon = {
                    IconButton(onClick = { viewModel.refreshAllStatuses() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = Color.White)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Greeting and device name section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = greeting,
                        color = TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = activeDevice?.name ?: "No active device",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (activeDevice != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val status = viewModel.deviceStatuses[activeDevice.id] ?: DeviceOnlineStatus.Unknown
                        val isOnline = status == DeviceOnlineStatus.Online
                        
                        // Check if UDP broadcast was received in the last 15 seconds
                        val lastSeenUdp = viewModel.lastSeenBroadcastTime[activeDevice.name.trim().lowercase()] ?: 0L
                        val isUdpActive = (System.currentTimeMillis() - lastSeenUdp) < 15000L

                        val (bannerBg, bannerBorder, bannerText, bannerIcon, bannerLabel) = when {
                            isOnline && isUdpActive -> {
                                listOf(
                                    Color(0x2200C9A7),
                                    Color(0x8800C9A7),
                                    Color(0xFF00C9A7),
                                    Icons.Default.Wifi,
                                    "Auto-connected (UDP Active)"
                                )
                            }
                            isOnline -> {
                                listOf(
                                    Color(0x22FFB830),
                                    Color(0x88FFB830),
                                    Color(0xFFFFB830),
                                    Icons.Default.Computer,
                                    "Connected via saved IP"
                                )
                            }
                            status == DeviceOnlineStatus.Checking -> {
                                listOf(
                                    Color(0x228899AA),
                                    Color(0x888899AA),
                                    Color(0xFF8899AA),
                                    Icons.Default.Search,
                                    "Scanning for laptop..."
                                )
                            }
                            else -> {
                                listOf(
                                    Color(0x22FF4757),
                                    Color(0x88FF4757),
                                    Color(0xFFFF4757),
                                    Icons.Default.Warning,
                                    "Device not found (Offline)"
                                )
                            }
                        }

                        Card(
                            border = BorderStroke(1.dp, bannerBorder as Color),
                            colors = CardDefaults.cardColors(containerColor = bannerBg as Color),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { viewModel.checkDeviceStatus(activeDevice) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = bannerIcon as ImageVector,
                                    contentDescription = null,
                                    tint = bannerText as Color,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = bannerLabel as String,
                                    color = bannerText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Device status card
                if (activeDevice != null) {
                    val status = viewModel.deviceStatuses[activeDevice.id] ?: DeviceOnlineStatus.Unknown
                    val isOnline = status == DeviceOnlineStatus.Online
                    val statusText = when (status) {
                        DeviceOnlineStatus.Online -> "Online"
                        DeviceOnlineStatus.Offline -> "Offline"
                        DeviceOnlineStatus.Checking -> "Checking..."
                        else -> "Unknown"
                    }
                    val statusColor = if (isOnline) TealAccent else if (status == DeviceOnlineStatus.Checking) Color.Yellow else ShutdownRed

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardSurface)
                            .clickable { viewModel.checkDeviceStatus(activeDevice) }
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ACTIVE DEVICE",
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = activeDevice.name,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                // Status dot (Pulsing)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .scale(if (isOnline) 1f + (breathingPulse * 0.4f) else 1f)
                                            .background(statusColor, CircleShape)
                                    )
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "IP ADDRESS", color = TextMuted, fontSize = 11.sp)
                                    // Partially masked IP e.g. 192.168.x.x
                                    val ipParts = activeDevice.ipAddress.split(".")
                                    val maskedIp = if (ipParts.size == 4) {
                                        "${ipParts[0]}.${ipParts[1]}.••.••"
                                    } else {
                                        "192.168.••.••"
                                    }
                                    Text(
                                        text = maskedIp,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "LAST SEEN", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        text = if (isOnline) "Just now" else "Offline",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardSurface)
                            .clickable { onNavigateToDevices() }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AddHome, contentDescription = "Add", tint = TealAccent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap to configure device",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Volume and Brightness Sliders Card
                if (activeDevice != null) {
                    val status = viewModel.deviceStatuses[activeDevice.id] ?: DeviceOnlineStatus.Unknown
                    val isOnline = status == DeviceOnlineStatus.Online
                    val volume by viewModel.volumeLevel.collectAsState()
                    val brightness by viewModel.brightnessLevel.collectAsState()

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardSurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "DEVICE ADJUSTMENTS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )

                            // Volume Controller
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                volume == 0 -> Icons.Default.VolumeMute
                                                volume < 40 -> Icons.Default.VolumeDown
                                                else -> Icons.Default.VolumeUp
                                            },
                                            contentDescription = "Volume",
                                            tint = if (isOnline) TealAccent else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Volume",
                                            color = if (isOnline) Color.White else TextMuted,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "$volume%",
                                        color = if (isOnline) TealAccent else TextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = volume.toFloat(),
                                    onValueChange = { if (isOnline) viewModel.setVolume(activeDevice, it.toInt()) },
                                    valueRange = 0f..100f,
                                    enabled = isOnline,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = TealAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                        thumbColor = TealAccent,
                                        disabledActiveTrackColor = TextMuted.copy(alpha = 0.3f),
                                        disabledThumbColor = TextMuted
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Divider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)

                            // Brightness Controller
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.BrightnessMedium,
                                            contentDescription = "Brightness",
                                            tint = if (isOnline) TealAccent else TextMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Brightness",
                                            color = if (isOnline) Color.White else TextMuted,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "$brightness%",
                                        color = if (isOnline) TealAccent else TextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = brightness.toFloat(),
                                    onValueChange = { if (isOnline) viewModel.setBrightness(activeDevice, it.toInt()) },
                                    valueRange = 0f..100f,
                                    enabled = isOnline,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = TealAccent,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                        thumbColor = TealAccent,
                                        disabledActiveTrackColor = TextMuted.copy(alpha = 0.3f),
                                        disabledThumbColor = TextMuted
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (!isOnline) {
                                Text(
                                    text = "Connect device to adjust sliders",
                                    color = ShutdownRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }

                // CENTER: Large Circular Shutdown Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                ) {
                    // Glow background layer
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .background(
                                color = ShutdownRed.copy(alpha = if (isPowerButtonPressed) 0.25f else 0.12f),
                                shape = CircleShape
                            )
                    )

                    // Button body with shadow and custom spring scale pointer detector
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(130.dp)
                            .scale(powerButtonScale)
                            .clip(CircleShape)
                            .background(ShutdownRed)
                            .pointerInput(activeDevice) {
                                detectTapGestures(
                                    onPress = {
                                        if (activeDevice == null) return@detectTapGestures
                                        isPowerButtonPressed = true
                                        tryAwaitRelease()
                                        isPowerButtonPressed = false
                                        selectedActionState = "shutdown"
                                        showConfirmModal = true
                                    }
                                )
                            }
                    ) {
                        val isOp = viewModel.isCommandInProgress.collectAsState().value
                        if (isOp && selectedActionState == "shutdown") {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(50.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Trigger Remote command",
                                tint = Color.White,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Press button to trigger command",
                    color = TextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Secondary actions pills row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryPillButton(
                        icon = Icons.Default.Bedtime,
                        label = "Sleep",
                        onClick = {
                            if (activeDevice != null) {
                                selectedActionState = "sleep"
                                showConfirmModal = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryPillButton(
                        icon = Icons.Default.RestartAlt,
                        label = "Restart",
                        onClick = {
                            if (activeDevice != null) {
                                selectedActionState = "restart"
                                showConfirmModal = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryPillButton(
                        icon = Icons.Default.Lock,
                        label = "Lock",
                        onClick = {
                            if (activeDevice != null) {
                                selectedActionState = "lock"
                                showConfirmModal = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom sheet confirm modal
            if (showConfirmModal && activeDevice != null) {
                ConfirmBottomSheet(
                    deviceName = activeDevice.name,
                    action = selectedActionState,
                    onConfirm = {
                        showConfirmModal = false
                        viewModel.sendCommand(activeDevice, selectedActionState)
                    },
                    onDismiss = { showConfirmModal = false }
                )
            }
        }
    }
}

@Composable
fun SecondaryPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        modifier = modifier
            .height(105.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = TealAccent,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmBottomSheet(
    deviceName: String,
    action: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionTextByLabel = when (action) {
        "shutdown" -> "Shut down"
        "sleep" -> "Sleep"
        "restart" -> "Restart"
        "lock" -> "Lock"
        else -> "Instruct"
    }

    val actionDesc = when (action) {
        "shutdown" -> "This will immediately power off your device."
        "sleep" -> "This will trigger diagnostic sleep state."
        "restart" -> "This will reboot your device safely."
        "lock" -> "This will trigger immediate session screenlock."
        else -> ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(ShutdownRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (action) {
                        "sleep" -> Icons.Default.Bedtime
                        "restart" -> Icons.Default.RestartAlt
                        "lock" -> Icons.Default.Lock
                        else -> Icons.Default.PowerSettingsNew
                    },
                    contentDescription = actionTextByLabel,
                    tint = ShutdownRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text titles
            Text(
                text = "$actionTextByLabel $deviceName?",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = actionDesc,
                color = TextMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Actions buttons stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = ShutdownRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "Yes, execute action", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, TextMuted.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(text = "Cancel", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
