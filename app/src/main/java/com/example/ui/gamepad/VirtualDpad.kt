package com.example.ui.gamepad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
fun VirtualDpad(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    upBtn: GameButton = GameButton.UP,
    downBtn: GameButton = GameButton.DOWN,
    leftBtn: GameButton = GameButton.LEFT,
    rightBtn: GameButton = GameButton.RIGHT,
    accentColor: Color = NeonCyan,
    tag: String = "virtual_dpad",
    onButtonPress: (button: GameButton, pressed: Boolean) -> Unit
) {
    val wingSize = size / 3f

    Box(
        modifier = modifier
            .size(size)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        // Cross background
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF0C1424))
                .border(1.dp, Color(0xFF1E2F4D), CircleShape)
        )

        // Center hub
        Box(
            modifier = Modifier
                .size(wingSize * 0.9f)
                .clip(CircleShape)
                .background(Color(0xFF131D31))
                .border(1.dp, accentColor.copy(alpha = 0.3f), CircleShape)
        )

        // Up Wing
        DpadWing(
            button = upBtn,
            label = "▲",
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(wingSize),
            onPress = onButtonPress
        )

        // Down Wing
        DpadWing(
            button = downBtn,
            label = "▼",
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(wingSize),
            onPress = onButtonPress
        )

        // Left Wing
        DpadWing(
            button = leftBtn,
            label = "◀",
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(wingSize),
            onPress = onButtonPress
        )

        // Right Wing
        DpadWing(
            button = rightBtn,
            label = "▶",
            accentColor = accentColor,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(wingSize),
            onPress = onButtonPress
        )
    }
}

@Composable
private fun DpadWing(
    button: GameButton,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onPress: (GameButton, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .testTag("dpad_${button.name}")
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.verticalGradient(
                    if (isPressed) {
                        listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.7f))
                    } else {
                        listOf(Color(0xFF1F2E4A), Color(0xFF142036))
                    }
                )
            )
            .border(
                width = 1.dp,
                color = if (isPressed) accentColor else Color(0xFF2A3D63),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(button) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPress(button, true)

                    val upOrCancel = waitForUpOrCancellation()
                    upOrCancel?.consume()
                    isPressed = false
                    onPress(button, false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else accentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

