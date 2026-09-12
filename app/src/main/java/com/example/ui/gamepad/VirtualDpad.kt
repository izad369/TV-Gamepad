package com.example.ui.gamepad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
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
    size: Dp = 145.dp,
    upBtn: GameButton = GameButton.UP,
    downBtn: GameButton = GameButton.DOWN,
    leftBtn: GameButton = GameButton.LEFT,
    rightBtn: GameButton = GameButton.RIGHT,
    accentColor: Color = NeonCyan,
    tag: String = "virtual_dpad",
    onButtonPress: (button: GameButton, pressed: Boolean) -> Unit
) {
    val wingSize = size * 0.35f
    Box(modifier = modifier.size(size).testTag(tag), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(Color(0xFF0D1627)).border(1.5.dp, Color(0xFF203254), CircleShape)
        )
        Box(
            modifier = Modifier.size(wingSize * 0.85f).clip(CircleShape).background(Color(0xFF16233B)).border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
        )
        DpadWing(button = upBtn, label = "▲", accentColor = accentColor, modifier = Modifier.align(Alignment.TopCenter).size(wingSize), onPress = onButtonPress)
        DpadWing(button = downBtn, label = "▼", accentColor = accentColor, modifier = Modifier.align(Alignment.BottomCenter).size(wingSize), onPress = onButtonPress)
        DpadWing(button = leftBtn, label = "◀", accentColor = accentColor, modifier = Modifier.align(Alignment.CenterStart).size(wingSize), onPress = onButtonPress)
        DpadWing(button = rightBtn, label = "▶", accentColor = accentColor, modifier = Modifier.align(Alignment.CenterEnd).size(wingSize), onPress = onButtonPress)
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
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "dpad_scale_${button.name}"
    )
    Box(
        modifier = modifier
            .scale(scale)
            .testTag("dpad_${button.name}")
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(if (isPressed) listOf(accentColor.copy(alpha = 0.6f), accentColor.copy(alpha = 0.9f)) else listOf(Color(0xFF223456), Color(0xFF142138))))
            .border(1.5.dp, if (isPressed) Color.White else accentColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
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
        Text(text = label, color = if (isPressed) Color.White else accentColor, fontSize = 17.sp, fontWeight = FontWeight.Black)
    }
}
