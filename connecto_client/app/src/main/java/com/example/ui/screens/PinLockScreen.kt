package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.ShutdownRed
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PinLockScreen(
    correctPin: String,
    onSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val isErrorShake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun handleKeyPress(digit: String) {
        if (enteredPin.length < 4) {
            enteredPin += digit
            errorMessage = ""
        }
        
        if (enteredPin.length == 4) {
            scope.launch {
                delay(200) // Small organic delay for feedback
                if (enteredPin == correctPin) {
                    onSuccess()
                } else {
                    errorMessage = "Incorrect PIN. Try again."
                    enteredPin = ""
                    // Play shake animation
                    isErrorShake.animateTo(
                        targetValue = 15f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioHighBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    isErrorShake.animateTo(0f)
                }
            }
        }
    }

    fun handleDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp)
            .offset(x = isErrorShake.value.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header Lock Icon & Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(TealAccent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Shield",
                        tint = TealAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter Access PIN",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (errorMessage.isNotEmpty()) errorMessage else "Security lock enabled",
                    color = if (errorMessage.isNotEmpty()) ShutdownRed else TextMuted,
                    fontSize = 14.sp
                )
            }

            // Circle Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 32.dp)
            ) {
                for (i in 0 until 4) {
                    val isActive = enteredPin.length > i
                    val color = if (isActive) TealAccent else Color.White.copy(alpha = 0.2f)
                    val sizeValue = if (isActive) 18.dp else 14.dp
                    Box(
                        modifier = Modifier
                            .size(sizeValue)
                            .background(color, CircleShape)
                    )
                }
            }

            // Custom Keypad Matrix
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                val grid = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "delete")
                )

                for (row in grid) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (cell in row) {
                            if (cell.isEmpty()) {
                                Spacer(modifier = Modifier.size(72.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            if (cell == "delete") {
                                                handleDelete()
                                            } else {
                                                handleKeyPress(cell)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cell == "delete") {
                                        Text(
                                            text = "⌫",
                                            color = Color.White,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = cell,
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
