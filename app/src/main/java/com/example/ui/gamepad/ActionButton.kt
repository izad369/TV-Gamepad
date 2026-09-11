package com.example.ui.gamepad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameButton
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow

@Composable
fun ActionButton(
    button: GameButton,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onPressChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .testTag("action_btn_${button.name}")
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                ambientColor = primaryColor,
                spotColor = primaryColor
            )
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(primaryColor.copy(alpha = 0.5f), primaryColor.copy(alpha = 0.8f))
                    } else {
                        listOf(Color(0xFF1E2B45), Color(0xFF10192A))
                    }
                )
            )
            .border(
                width = 2.dp,
                color = if (isPressed) primaryColor else primaryColor.copy(alpha = 0.6f),
                shape = CircleShape
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
            text = button.label,
            color = if (isPressed) Color.White else primaryColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black
        )
    }
}

/**
 * Diamond formation of 4 main action buttons (Y at top, X on left, B on right, A at bottom).
 */
@Composable
fun ActionButtonsDiamond(
    size: Dp = 170.dp,
    modifier: Modifier = Modifier,
    onButtonPress: (button: GameButton, pressed: Boolean) -> Unit
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Top: Y (Yellow)
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            ActionButton(button = GameButton.Y, primaryColor = NeonYellow, onPressChange = { onButtonPress(GameButton.Y, it) })
        }
        // Left: X (Blue)
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            ActionButton(button = GameButton.X, primaryColor = NeonBlue, onPressChange = { onButtonPress(GameButton.X, it) })
        }
        // Right: B (Red)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            ActionButton(button = GameButton.B, primaryColor = NeonRed, onPressChange = { onButtonPress(GameButton.B, it) })
        }
        // Bottom: A (Green)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ActionButton(button = GameButton.A, primaryColor = NeonGreen, onPressChange = { onButtonPress(GameButton.A, it) })
        }
    }
}

/**
 * Pill-shaped auxiliary buttons (SELECT, MENU, START).
 */
@Composable
fun AuxButton(
    label: String,
    modifier: Modifier = Modifier,
    onPressChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) NeonCyan.copy(alpha = 0.35f) else Color(0xFF1E2A3F))
            .border(1.dp, if (isPressed) NeonCyan else Color(0xFF334155), RoundedCornerShape(12.dp))
            .pointerInput(label) {
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
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("aux_btn_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) NeonCyan else Color.LightGray,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
