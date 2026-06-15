package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeviceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: DeviceViewModel
) {
    var showSetupAgentDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var pinValueState by remember { mutableStateOf(viewModel.settings.pinCode) }

    // Read initial preference states
    var pinLockEnabled by remember { mutableStateOf(viewModel.settings.isPinLockEnabled) }
    var biometricsEnabled by remember { mutableStateOf(viewModel.settings.isBiometricsEnabled) }
    var shutdownAlertsEnabled by remember { mutableStateOf(viewModel.settings.isShutdownAlertsEnabled) }
    var connectionLostAlertsEnabled by remember { mutableStateOf(viewModel.settings.isConnectionLostAlertsEnabled) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Section: Security
            SettingsSectionHeader(title = "Security")
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Key,
                        title = "Change Access PIN",
                        subtitle = "Current PIN: ${viewModel.settings.pinCode}",
                        onClick = { showPinChangeDialog = true }
                    )
                    Divider(color = DarkBg.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.Lock,
                        title = "Enable PIN Lock",
                        subtitle = "Prompt code on app launch",
                        checked = pinLockEnabled,
                        onCheckedChange = {
                            pinLockEnabled = it
                            viewModel.settings.isPinLockEnabled = it
                        }
                    )
                    Divider(color = DarkBg.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.Fingerprint,
                        title = "Biometric Authentication",
                        subtitle = "Unlock with fingerprint",
                        checked = biometricsEnabled,
                        onCheckedChange = {
                            biometricsEnabled = it
                            viewModel.settings.isBiometricsEnabled = it
                        }
                    )
                }
            }

            // Section: Notifications
            SettingsSectionHeader(title = "Notifications")
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsToggleItem(
                        icon = Icons.Default.PowerSettingsNew,
                        title = "Shutdown Alerts",
                        subtitle = "Notify when command is successfully sent",
                        checked = shutdownAlertsEnabled,
                        onCheckedChange = {
                            shutdownAlertsEnabled = it
                            viewModel.settings.isShutdownAlertsEnabled = it
                        }
                    )
                    Divider(color = DarkBg.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsToggleItem(
                        icon = Icons.Default.WifiOff,
                        title = "Connection Lost Alert",
                        subtitle = "Notify when active device goes offline",
                        checked = connectionLostAlertsEnabled,
                        onCheckedChange = {
                            connectionLostAlertsEnabled = it
                            viewModel.settings.isConnectionLostAlertsEnabled = it
                        }
                    )
                }
            }

            // Section: About
            SettingsSectionHeader(title = "About")
            Card(
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = "Version",
                        subtitle = "Connecto v1.0.0",
                        onClick = {}
                    )
                    Divider(color = DarkBg.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        icon = Icons.Default.Code,
                        title = "How to set up the laptop agent",
                        subtitle = "View Node.js & Python guidelines",
                        onClick = { showSetupAgentDialog = true }
                    )
                    Divider(color = DarkBg.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        icon = Icons.Default.OpenInNew,
                        title = "GitHub",
                        subtitle = "Connecto official repository",
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Agent Setup Tutorial Dialog
    if (showSetupAgentDialog) {
        AlertDialog(
            onDismissRequest = { showSetupAgentDialog = false },
            title = { Text(text = "Laptop Agent Setup", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "To allow Connecto to trigger remote commands, download and run the lightweight agent on your target computers.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1. Node.js Express Example:",
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "const express = require('express');\nconst app = express();\n\napp.post('/shutdown', (req, res) => {\n  require('child_process').exec('shutdown /s');\n  res.send('Shutting down...');\n});\n\napp.listen(5000);",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "2. Header Security Token:",
                        fontWeight = FontWeight.Bold,
                        color = TealAccent,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Verify that the Bearer authorization header matches the secret token defined inside the device configuration screen.",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSetupAgentDialog = false }) {
                    Text(text = "Got it", color = TealAccent, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CardSurface
        )
    }

    // Change PIN Dialog
    if (showPinChangeDialog) {
        AlertDialog(
            onDismissRequest = { showPinChangeDialog = false },
            title = { Text(text = "Update Security PIN", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Enter a new 4-digit code to protect access:", color = TextMuted)
                    OutlinedTextField(
                        value = pinValueState,
                        onValueChange = { if (it.length <= 4) pinValueState = it },
                        placeholder = { Text(text = "1111") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = DarkBg,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinValueState.length == 4) {
                            viewModel.settings.pinCode = pinValueState
                            showPinChangeDialog = false
                        }
                    },
                    enabled = pinValueState.length == 4
                ) {
                    Text(text = "Save PIN", color = TealAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinChangeDialog = false }) {
                    Text(text = "Cancel", color = Color.White)
                }
            },
            containerColor = CardSurface
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = TealAccent,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = TextMuted)
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted)
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = TextMuted)
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = TextMuted, fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkBg,
                checkedTrackColor = TealAccent,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardSurface
            )
        )
    }
}
