package com.example.ui.gamepad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    knobRatio: Float = 0.38f,
    deadzone: Float = 0.05f,
    accentColor: Color = NeonCyan,
    tag: String = "virtual_joystick",
    onMove: (x: Float, y: Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag(tag)
            .pointerInput(Unit) {
                val radius = this.size.width / 2f
                val maxDistance = radius * (1f - knobRatio / 2f)

                fun handlePointer(pos: Offset) {
                    val center = Offset(radius, radius)
                    val delta = pos - center
                    val dist = delta.getDistance()
                    val clampedDist = min(dist, maxDistance)
                    val angle = atan2(delta.y, delta.x)
                    val clampedOffset = Offset(
                        clampedDist * cos(angle),
                        clampedDist * sin(angle)
                    )
                    knobOffset = clampedOffset

                    val normDist = (clampedDist / maxDistance).coerceIn(0f, 1f)
                    if (normDist > deadzone) {
                        val normX = (clampedOffset.x / maxDistance).coerceIn(-1f, 1f)
                        val normY = (clampedOffset.y / maxDistance).coerceIn(-1f, 1f)
                        onMove(normX, normY)
                    } else {
                        onMove(0f, 0f)
                    }
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    handlePointer(down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            change.consume()
                            handlePointer(change.position)
                        } else {
                            break
                        }
                    }

                    knobOffset = Offset.Zero
                    onMove(0f, 0f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val outerRadius = size.toPx() / 2f
            val knobRadius = outerRadius * knobRatio

            // Outer Base Ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF142036), Color(0xFF0C1422)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer Border
            drawCircle(
                color = accentColor.copy(alpha = 0.55f),
                radius = outerRadius - 2f,
                center = center,
                style = Stroke(width = 3f)
            )

            // Cross guide lines
            drawLine(
                color = Color(0xFF223554),
                start = Offset(center.x - outerRadius * 0.7f, center.y),
                end = Offset(center.x + outerRadius * 0.7f, center.y),
                strokeWidth = 2f
            )
            drawLine(
                color = Color(0xFF223554),
                start = Offset(center.x, center.y - outerRadius * 0.7f),
                end = Offset(center.x, center.y + outerRadius * 0.7f),
                strokeWidth = 2f
            )

            // Inner Deadzone Ring
            drawCircle(
                color = Color(0xFF1C2C45),
                radius = outerRadius * 0.25f,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Thumb Knob
            val currentKnobCenter = center + knobOffset
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor, accentColor.copy(alpha = 0.6f)),
                    center = currentKnobCenter,
                    radius = knobRadius
                ),
                radius = knobRadius,
                center = currentKnobCenter
            )

            // Knob Border
            drawCircle(
                color = Color.White,
                radius = knobRadius,
                center = currentKnobCenter,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
