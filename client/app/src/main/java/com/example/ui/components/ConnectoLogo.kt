package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TextMuted

// Color Palette corresponding to the Connecto Brand
val BrandBlue = Color(0xFF0082FF)
val BrandSkyBlue = Color(0xFF00C3FF)
val BrandPurple = Color(0xFF9E52FF)
val BrandViolet = Color(0xFF6B11FF)
val BrandPink = Color(0xFFD611FF)

@Composable
fun ConnectoLogo(
    modifier: Modifier = Modifier,
    animate: Boolean = true
) {
    // Dynamic transition for ambient glowing or subtle breathing animation
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val breathScale by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breath"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val rotationAngle by if (animate) {
        infiniteTransition.animateFloat(
            initialValue = -3f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotate"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    Canvas(
        modifier = modifier
            .graphicsLayer {
                scaleX = breathScale
                scaleY = breathScale
                rotationZ = rotationAngle
            }
    ) {
        val width = size.width
        val height = size.height
        val midX = width / 2f
        val midY = height / 2f

        // Geometry sizing
        val radius = size.minDimension * 0.24f
        val strokeWidth = radius * 0.38f

        val leftOffset = radius * 0.65f
        val leftCenter = Offset(midX - leftOffset, midY)
        val rightCenter = Offset(midX + leftOffset, midY)

        // 1. Gradients
        val leftGradient = Brush.linearGradient(
            colors = listOf(BrandBlue, BrandSkyBlue),
            start = Offset(midX - leftOffset * 2, midY - radius),
            end = Offset(midX, midY + radius)
        )

        val rightGradient = Brush.linearGradient(
            colors = listOf(BrandPurple, BrandViolet),
            start = Offset(midX, midY - radius),
            end = Offset(midX + leftOffset * 2, midY + radius)
        )

        val middleGradient = Brush.linearGradient(
            colors = listOf(BrandBlue, BrandPurple),
            start = Offset(midX - leftOffset * 0.7f, midY),
            end = Offset(midX + leftOffset * 0.7f, midY)
        )

        // 2. Draw Left C Ring
        // Starting at 52 degrees and sweeping 256 degrees (fills bottom, left, top and stops at top-right)
        drawArc(
            brush = leftGradient,
            startAngle = 54f,
            sweepAngle = 252f,
            useCenter = false,
            topLeft = Offset(leftCenter.x - radius, leftCenter.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Draw Right Inverse C Ring
        // Starting at 234 degrees and sweeping 252 degrees (fills top, right, bottom and stops at bottom-left)
        drawArc(
            brush = rightGradient,
            startAngle = 234f,
            sweepAngle = 252f,
            useCenter = false,
            topLeft = Offset(rightCenter.x - radius, rightCenter.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 4. Draw Center Connector Bar (Horizontal Pill shape)
        val barWidth = radius * 1.3f
        val barHeight = strokeWidth * 1.0f
        val barLeft = midX - barWidth / 2f
        val barTop = midY - barHeight / 2f

        val pillPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = barLeft,
                    top = barTop,
                    right = barLeft + barWidth,
                    bottom = barTop + barHeight,
                    radiusX = barHeight / 2f,
                    radiusY = barHeight / 2f
                )
            )
        }

        drawPath(
            path = pillPath,
            brush = middleGradient
        )
    }
}

@Composable
fun ConnectoLogoWithText(
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    logoSize: Int = 140,
    fontSizeValue: Int = 36,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ConnectoLogo(
            modifier = Modifier.size(logoSize.dp),
            animate = animate
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        // Styled lowercase "connecto" text matching logo image
        // 'c-o-n-n-e-c-t' is white, 'o' is white with top-right purple accent
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                append("connect")
            }
            // Stylize 'o' with a violet tint on the last character or whole violet accent
            withStyle(style = SpanStyle(color = BrandPurple, fontWeight = FontWeight.Black)) {
                append("o")
            }
        }

        Text(
            text = annotatedText,
            fontSize = fontSizeValue.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = Color.White
        )

        if (showTagline) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your devices, connected.",
                color = TextMuted,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}
