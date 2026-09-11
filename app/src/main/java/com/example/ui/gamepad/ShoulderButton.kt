package com.example.ui.gamepad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameButton
import com.example.ui.theme.NeonCyan

@Composable
fun ShoulderButton(
    button: GameButton,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    height: Dp = 38.dp,
    accentColor: Color = NeonCyan,
    onPressChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "shoulder_scale"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .scale(scale)
            .testTag("shoulder_${button.name}")
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.verticalGradient(
                    if (isPressed) {
                        listOf(accentColor.copy(alpha = 0.45f), accentColor.copy(alpha = 0.75f))
                    } else {
                        listOf(Color(0xFF1F2C45), Color(0xFF121B2C))
                    }
                )
            )
            .border(
                width = 1.5.dp,
                color = if (isPressed) accentColor else Color(0xFF2E4065),
                shape = RoundedCornerShape(10.dp)
            )
            .pointerInput(button) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPressChange(true)

                    val upOrCancel = waitForUpOrCancellation()
                    upOrCancel?.consume()
                    isPressed = false
                    onPressChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = button.name,
            color = if (isPressed) Color.White else accentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
