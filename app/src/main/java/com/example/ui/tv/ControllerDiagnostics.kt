package com.example.ui.tv

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.focusable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ControllerPlayer
import com.example.model.GameButton
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow

@Composable
fun ControllerDiagnostics(
    players: List<ControllerPlayer>,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val player = players.firstOrNull()
    val input = player?.inputState

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    var isBackFocused by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = onExit,
                        modifier = Modifier
                            .testTag("exit_diag_btn")
                            .onFocusChanged { isBackFocused = it.isFocused }
                            .focusable()
                            .background(if (isBackFocused) NeonCyan.copy(alpha = 0.3f) else Color.Transparent, CircleShape)
                            .border(if (isBackFocused) 2.dp else 0.dp, NeonCyan, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = if (isBackFocused) NeonCyan else Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "CONTROLLER DIAGNOSTICS & TEST LAB",
                            color = NeonCyan,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (player != null) "Testing: ${player.name} (${player.connectionType})" else "No controller connected. Connect your phone!",
                            color = if (player != null) NeonGreen else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Test Dashboard Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Panel: Stick & Tilt Coordinates
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ANALOG STICK VECTOR",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Visual Joystick Axis Map
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C1424))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(140.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            // Crosshairs
                            drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, center.y), Offset(size.width, center.y), 1f)
                            drawLine(Color.White.copy(alpha = 0.2f), Offset(center.x, 0f), Offset(center.x, size.height), 1f)

                            val stickX = input?.stickX ?: 0f
                            val stickY = input?.stickY ?: 0f
                            val knobPos = Offset(
                                center.x + stickX * (size.width / 2f - 14f),
                                center.y + stickY * (size.height / 2f - 14f)
                            )

                            // Pointer line
                            drawLine(NeonCyan, center, knobPos, 2f)
                            // Knob
                            drawCircle(NeonCyan, radius = 12f, center = knobPos)
                            drawCircle(Color.White, radius = 4f, center = knobPos)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "X: ${"%.2f".format(input?.stickX ?: 0f)}",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Y: ${"%.2f".format(input?.stickY ?: 0f)}",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Gyro / Tilt Indicator
                    Text(
                        text = "PHONE TILT / GYRO",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("Tilt X: ${"%.2f".format(input?.tiltX ?: 0f)}", color = NeonYellow, fontSize = 12.sp)
                        Text("Tilt Y: ${"%.2f".format(input?.tiltY ?: 0f)}", color = NeonYellow, fontSize = 12.sp)
                    }
                }

                // Center Panel: Interactive Button Map
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CyberCardBg)
                        .border(1.dp, CyberCardBorder, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "REAL-TIME BUTTON PRESSES",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Shoulders Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            TestButtonBadge("L2", input?.pressedButtons?.contains(GameButton.L2) == true)
                            Spacer(modifier = Modifier.width(6.dp))
                            TestButtonBadge("L1", input?.pressedButtons?.contains(GameButton.L1) == true)
                        }
                        Row {
                            TestButtonBadge("R1", input?.pressedButtons?.contains(GameButton.R1) == true)
                            Spacer(modifier = Modifier.width(6.dp))
                            TestButtonBadge("R2", input?.pressedButtons?.contains(GameButton.R2) == true)
                        }
                    }

                    // D-Pad and Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // D-Pad
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TestButtonBadge("▲", input?.pressedButtons?.contains(GameButton.UP) == true)
                            Row {
                                TestButtonBadge("◀", input?.pressedButtons?.contains(GameButton.LEFT) == true)
                                Spacer(modifier = Modifier.width(8.dp))
                                TestButtonBadge("▶", input?.pressedButtons?.contains(GameButton.RIGHT) == true)
                            }
                            TestButtonBadge("▼", input?.pressedButtons?.contains(GameButton.DOWN) == true)
                        }

                        // Center auxiliary
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TestButtonBadge("SELECT", input?.pressedButtons?.contains(GameButton.SELECT) == true, isSmall = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            TestButtonBadge("MENU", input?.pressedButtons?.contains(GameButton.MENU) == true, isSmall = true)
                            Spacer(modifier = Modifier.height(4.dp))
                            TestButtonBadge("START", input?.pressedButtons?.contains(GameButton.START) == true, isSmall = true)
                        }

                        // Diamond Action Buttons
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TestButtonBadge("Y", input?.pressedButtons?.contains(GameButton.Y) == true, activeColor = NeonYellow)
                            Row {
                                TestButtonBadge("X", input?.pressedButtons?.contains(GameButton.X) == true, activeColor = NeonBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                TestButtonBadge("B", input?.pressedButtons?.contains(GameButton.B) == true, activeColor = NeonRed)
                            }
                            TestButtonBadge("A", input?.pressedButtons?.contains(GameButton.A) == true, activeColor = NeonGreen)
                        }
                    }

                    // Trigger Depth Gauges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("L2 Trigger: ${(input?.l2Value ?: 0f) * 100}%", color = Color.Gray, fontSize = 10.sp)
                            LinearProgressIndicator(
                                progress = { input?.l2Value ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("R2 Trigger: ${(input?.r2Value ?: 0f) * 100}%", color = Color.Gray, fontSize = 10.sp)
                            LinearProgressIndicator(
                                progress = { input?.r2Value ?: 0f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = NeonCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestButtonBadge(
    label: String,
    isActive: Boolean,
    activeColor: Color = NeonCyan,
    isSmall: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(if (isSmall) 42.dp else 36.dp, if (isSmall) 22.dp else 36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) activeColor else Color(0xFF16233B))
            .border(1.dp, if (isActive) Color.White else Color(0xFF26395B), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) Color.Black else Color.LightGray,
            fontSize = if (isSmall) 9.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
