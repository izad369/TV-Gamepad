package com.example.ui.gamepad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow

@Composable
fun ActionButton(
    button: GameButton,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    onPressChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "btn_scale_${button.name}"
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
                        listOf(primaryColor.copy(alpha = 0.65f), primaryColor.copy(alpha = 0.95f))
                    } else {
                        listOf(Color(0xFF223252), Color(0xFF141F33))
                    }
                )
            )
            .border(
                width = 2.5.dp,
                color = if (isPressed) Color.White else primaryColor,
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
            fontSize = if (size < 48.dp) 17.sp else 21.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun ActionButtonsDiamond(
    size: Dp = 150.dp,
    modifier: Modifier = Modifier,
    onButtonPress: (button: GameButton, pressed: Boolean) -> Unit
) {
    val btnSize = size * 0.36f
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            ActionButton(button = GameButton.Y, primaryColor = NeonYellow, size = btnSize, onPressChange = { onButtonPress(GameButton.Y, it) })
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            ActionButton(button = GameButton.X, primaryColor = NeonBlue, size = btnSize, onPressChange = { onButtonPress(GameButton.X, it) })
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            ActionButton(button = GameButton.B, primaryColor = NeonRed, size = btnSize, onPressChange = { onButtonPress(GameButton.B, it) })
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            ActionButton(button = GameButton.A, primaryColor = NeonGreen, size = btnSize, onPressChange = { onButtonPress(GameButton.A, it) })
        }
    }
}

@Composable
fun ActionButtonsWithZ(
    modifier: Modifier = Modifier,
    size: Dp = 155.dp,
    onButtonPress: (button: GameButton, pressed: Boolean) -> Unit
) {
    val btnSize = size * 0.31f
    Column(
        modifier = modifier.size(size),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(button = GameButton.X, primaryColor = NeonBlue, size = btnSize, onPressChange = { onButtonPress(GameButton.X, it) })
            ActionButton(button = GameButton.Y, primaryColor = NeonYellow, size = btnSize, onPressChange = { onButtonPress(GameButton.Y, it) })
            ActionButton(button = GameButton.Z, primaryColor = NeonPurple, size = btnSize, onPressChange = { onButtonPress(GameButton.Z, it) })
        }
        Row(
            modifier = Modifier.fillMaxWidth(0.78f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton(button = GameButton.A, primaryColor = NeonGreen, size = btnSize * 1.08f, onPressChange = { onButtonPress(GameButton.A, it) })
            ActionButton(button = GameButton.B, primaryColor = NeonRed, size = btnSize * 1.08f, onPressChange = { onButtonPress(GameButton.B, it) })
        }
    }
}

@Composable
fun AuxButton(
    label: String,
    modifier: Modifier = Modifier,
    onPressChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) NeonCyan.copy(alpha = 0.45f) else Color(0xFF1C2840))
            .border(1.5.dp, if (isPressed) NeonCyan else Color(0xFF384D70), RoundedCornerShape(10.dp))
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
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag("aux_btn_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = if (isPressed) Color.White else Color(0xFFE2E8F0), fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun ShoulderButton(
    button: GameButton,
    modifier: Modifier = Modifier,
    onPressChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val isTrigger = button == GameButton.L2 || button == GameButton.R2
    val accent = if (isTrigger) NeonPurple else NeonCyan
    Box(
        modifier = modifier
            .size(width = 68.dp, height = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) accent.copy(alpha = 0.4f) else Color(0xFF1B283E))
            .border(1.5.dp, if (isPressed) accent else Color(0xFF334668), RoundedCornerShape(8.dp))
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
            }
            .testTag("shoulder_btn_${button.name}"),
        contentAlignment = Alignment.Center
    ) {
        Text(text = button.name, color = if (isPressed) Color.White else accent, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}
