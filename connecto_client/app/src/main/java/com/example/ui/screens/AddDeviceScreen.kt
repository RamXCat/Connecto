package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.DeviceViewModel

data class DeviceTypeOption(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    viewModel: DeviceViewModel,
    onNavigateBack: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) }
    
    // Form fields states
    var ipInput by remember { mutableStateOf("") }
    var tokenInput by remember { mutableStateOf("") }
    var isTokenVisible by remember { mutableStateOf(false) }
    
    var nameInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Laptop") } // "Laptop", "Desktop", "Server"

    // Test connection response states
    var connectionTestState by remember { mutableStateOf<String?>(null) } // null, "testing", success, failed
    var connectionTestMsg by remember { mutableStateOf("") }

    val headingText = when (currentStep) {
        1 -> "Endpoint Config"
        2 -> "Name & Type"
        else -> "Verify Settings"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Add Device", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(text = "SETUP PROGRESS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = headingText, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = "Step $currentStep of 3", color = TealAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Linear Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(CardSurface)
                    ) {
                        val progressFraction = when (currentStep) {
                            1 -> 0.33f
                            2 -> 0.66f
                            else -> 1f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressFraction)
                                .fillMaxHeight()
                                .background(TealAccent, CircleShape)
                        )
                    }
                }

                // Center Dynamic Form step switcher
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    when (currentStep) {
                        1 -> {
                            // Step 1: Connections details
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Laptop IP or URL",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = ipInput,
                                    onValueChange = { ipInput = it },
                                    placeholder = { Text(text = "192.168.1.x or device.ngrok.io", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = CardSurface,
                                        focusedContainerColor = CardSurface,
                                        unfocusedContainerColor = CardSurface,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Secret Token",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = tokenInput,
                                    onValueChange = { tokenInput = it },
                                    placeholder = { Text(text = "••••••••••••", color = TextMuted) },
                                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                            Icon(
                                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle token visibility",
                                                tint = Color.White
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = CardSurface,
                                        focusedContainerColor = CardSurface,
                                        unfocusedContainerColor = CardSurface,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Connection status testing panel
                                OutlinedButton(
                                    onClick = {
                                        if (ipInput.trim().isEmpty() || tokenInput.trim().isEmpty()) {
                                            connectionTestState = "error"
                                            connectionTestMsg = "Fields cannot be empty."
                                            return@OutlinedButton
                                        }
                                        connectionTestState = "testing"
                                        viewModel.testConnection(ipInput, tokenInput) { success, msg ->
                                            connectionTestState = if (success) "success" else "failed"
                                            connectionTestMsg = msg
                                        }
                                    },
                                    border = BorderStroke(1.dp, TealAccent),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                ) {
                                    if (connectionTestState == "testing") {
                                        CircularProgressIndicator(color = TealAccent, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = TealAccent)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Test Connection", color = TealAccent)
                                    }
                                }

                                if (connectionTestState != null && connectionTestState != "testing") {
                                    val logColor = if (connectionTestState == "success") TealAccent else ShutdownRed
                                    Text(
                                        text = connectionTestMsg,
                                        color = logColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    )
                                }

                                val discoveredList by viewModel.discoveredDevices.collectAsState()
                                if (discoveredList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "📡 DISCOVERED ON LOCAL NETWORK",
                                        color = TealAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    
                                    discoveredList.forEach { discovered ->
                                        val platformIcon = when (discovered.platform.lowercase()) {
                                            "linux" -> Icons.Default.Dns
                                            "desktop" -> Icons.Default.DesktopWindows
                                            else -> Icons.Default.Laptop
                                        }
                                        
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = CardSurface),
                                            shape = RoundedCornerShape(12.dp),
                                            onClick = {
                                                ipInput = discovered.ip
                                                nameInput = discovered.name
                                                selectedType = when (discovered.platform.lowercase()) {
                                                    "linux" -> "Server"
                                                    "desktop" -> "Desktop"
                                                    else -> "Laptop"
                                                 }
                                                connectionTestState = null
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .background(TealAccent.copy(alpha = 0.15f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = platformIcon,
                                                            contentDescription = null,
                                                            tint = TealAccent,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = discovered.name,
                                                            color = Color.White,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = discovered.ip,
                                                            color = TextMuted,
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                }
                                                
                                                Text(
                                                    text = "Auto-fill",
                                                    color = TealAccent,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // Step 2: Name & Type options
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Device name",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    placeholder = { Text(text = "Home Laptop, Office PC...", color = TextMuted) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = TealAccent,
                                        unfocusedBorderColor = CardSurface,
                                        focusedContainerColor = CardSurface,
                                        unfocusedContainerColor = CardSurface,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Device type selector",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val types = listOf(
                                        DeviceTypeOption("Laptop", "💻 Laptop", Icons.Default.Laptop),
                                        DeviceTypeOption("Desktop", "🖥️ Desktop", Icons.Default.DesktopWindows),
                                        DeviceTypeOption("Server", "🖳 Server", Icons.Default.Dns)
                                    )

                                    for (item in types) {
                                        val isSelected = selectedType == item.key
                                        val borderCol = if (isSelected) TealAccent else Color.Transparent
                                        val bgCol = if (isSelected) TealAccent.copy(alpha = 0.15f) else CardSurface

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = bgCol),
                                            border = BorderStroke(1.5.dp, borderCol),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(64.dp)
                                                .clickable { selectedType = item.key }
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = item.label,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            // Step 3: Summary card confirmation
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = "Please check details are correct before finalizing connection:",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "DEVICE NAME", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(text = nameInput, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Divider(color = DarkBg.copy(alpha = 0.4f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "IP ADDRESS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(text = ipInput, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Divider(color = DarkBg.copy(alpha = 0.4f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "DEVICE TYPE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(text = selectedType, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Divider(color = DarkBg.copy(alpha = 0.4f))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "SECURITY STATUS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "🔒 Encrypted", color = TealAccent, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Setup screen back / continue actions controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            border = BorderStroke(1.dp, CardSurface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text(text = "Back", color = Color.White, fontSize = 16.sp)
                        }
                    }

                    val buttonText = if (currentStep == 3) "Save Device" else "Continue"
                    Button(
                        onClick = {
                            if (currentStep < 3) {
                                if (currentStep == 1) {
                                    if (ipInput.trim().isEmpty() || tokenInput.trim().isEmpty()) {
                                        return@Button
                                    }
                                }
                                currentStep++
                            } else {
                                // Final check name
                                if (nameInput.trim().isEmpty()) {
                                    nameInput = selectedType + " Node"
                                }
                                viewModel.addDevice(nameInput, ipInput, tokenInput, selectedType)
                                onNavigateBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(16.dp),
                        enabled = if (currentStep == 1) ipInput.isNotEmpty() && tokenInput.isNotEmpty() else true,
                        modifier = Modifier
                            .weight(2f)
                            .height(56.dp)
                    ) {
                        Text(text = buttonText, color = DarkBg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
