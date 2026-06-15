package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBg
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextMuted
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(2600) // Display splash for 2.6s before transition
        onSplashComplete()
    }

    // Infinite transition for status dot glow pulsation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_splash")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse"
    )

    // Animated drawing fractional state (0.0 to 1.0) for the stitching line
    val lineDrawFraction = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        lineDrawFraction.animateTo(
            targetValue = 1f,
            animationSpec = tween(1800, easing = FastOutSlowInEasing)
        )
    }

    // Fade-in state for brand text & tagline
    var textAlpha by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        delay(400)
        textAlpha = 1f
    }
    val animatedTextAlpha by animateFloatAsState(
        targetValue = textAlpha,
        animationSpec = tween(1000),
        label = "text_fade_in"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Stitch Logo vector
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 20.dp)
            ) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                
                val leftNode = Offset(width * 0.25f, midY)
                val rightNode = Offset(width * 0.75f, midY)

                val controlX = width * 0.5f
                val controlY = midY - 30f // Arc curve height

                // Define the connection path (quadratic bezier curve)
                val stitchPath = Path().apply {
                    moveTo(leftNode.x, leftNode.y)
                    quadraticTo(controlX, controlY, rightNode.x, rightNode.y)
                }

                // Draw background path glow
                drawPath(
                    path = stitchPath,
                    color = TealAccent.copy(alpha = 0.15f * animatedTextAlpha),
                    style = Stroke(
                        width = 16f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(12f, 10f),
                            phase = (1f - lineDrawFraction.value) * 100f
                        )
                    )
                )

                // Draw the connecting dotted line (animated)
                drawPath(
                    path = stitchPath,
                    color = TealAccent.copy(alpha = 0.85f),
                    style = Stroke(
                        width = 4f,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(12f, 8f),
                            phase = (1f - lineDrawFraction.value) * 100f
                        )
                    )
                )

                // Left Dot Glow
                drawCircle(
                    color = TealAccent.copy(alpha = 0.25f),
                    radius = dotScale,
                    center = leftNode
                )
                
                // Left Solid Node
                drawCircle(
                    color = TealAccent,
                    radius = 12f,
                    center = leftNode
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = leftNode
                )

                // Right Dot Glow
                drawCircle(
                    color = TealAccent.copy(alpha = 0.25f),
                    radius = dotScale,
                    center = rightNode
                )

                // Right Solid Node
                drawCircle(
                    color = TealAccent,
                    radius = 12f,
                    center = rightNode
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = rightNode
                )
            }

            // App Name
            Text(
                text = "CONNECTO",
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Tagline
            Text(
                text = "Your devices, connected.",
                color = TextMuted,
                fontSize = 16.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }
    }
}
