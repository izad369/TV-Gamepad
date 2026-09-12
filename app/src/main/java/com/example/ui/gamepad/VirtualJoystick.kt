package com.example.ui.gamepad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    knobRatio: Float = 0.35f,
    deadzone: Float = 0.08f,
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

                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radius, radius)
                        val delta = offset - center
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
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(radius, radius)
                        val delta = change.position - center
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
                    },
                    onDragEnd = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        knobOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f - 4.dp.toPx()
            val knobRadius = this.size.width * knobRatio / 2f

            // Outer ring base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF131D31), Color(0xFF090E17)),
                    center = center,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Outer glowing border
            drawCircle(
                color = NeonCyan.copy(alpha = 0.35f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Cross guide lines
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(center.x - outerRadius * 0.7f, center.y),
                end = Offset(center.x + outerRadius * 0.7f, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(center.x, center.y - outerRadius * 0.7f),
                end = Offset(center.x, center.y + outerRadius * 0.7f),
                strokeWidth = 1.dp.toPx()
            )

            // Knob position
            val knobPos = center + knobOffset

            // Knob shadow/outer glow
            drawCircle(
                color = accentColor.copy(alpha = 0.3f),
                radius = knobRadius + 4.dp.toPx(),
                center = knobPos
            )

            // Knob body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2A3D63), Color(0xFF16233B)),
                    center = knobPos,
                    radius = knobRadius
                ),
                radius = knobRadius,
                center = knobPos
            )

            // Knob border & center dot
            drawCircle(
                color = accentColor,
                radius = knobRadius,
                center = knobPos,
                style = Stroke(width = 2.5.dp.toPx())
            )

            drawCircle(
                color = accentColor.copy(alpha = 0.8f),
                radius = 5.dp.toPx(),
                center = knobPos
            )
        }
    }
}
