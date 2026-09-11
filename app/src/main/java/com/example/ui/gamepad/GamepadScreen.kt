package com.example.ui.gamepad

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionStatus
import com.example.model.ConnectionType
import com.example.model.GameButton
import com.example.network.PhoneClientEngine
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow

enum class PhoneControlMode {
    GAMEPAD,
    TV_REMOTE
}

@Composable
fun GamepadScreen(
    clientEngine: PhoneClientEngine,
    onSwitchToTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConnectDialog by remember { mutableStateOf(false) }
    var useDpadInsteadOfStick by remember { mutableStateOf(false) }
    var motionEnabled by remember { mutableStateOf(false) }
    var currentControlMode by remember { mutableStateOf(PhoneControlMode.GAMEPAD) }

    val status by clientEngine.connectionStatus.collectAsStateWithLifecycle()
    val connType by clientEngine.connectionType.collectAsStateWithLifecycle()
    val hostName by clientEngine.connectedHostName.collectAsStateWithLifecycle()
    val pingMs by clientEngine.pingMs.collectAsStateWithLifecycle()

    val isHidRegistered by clientEngine.hidManager.isRegistered.collectAsStateWithLifecycle()
    val hidConnectedDevice by clientEngine.hidManager.connectedDevice.collectAsStateWithLifecycle()
    val isHidSupported by clientEngine.hidManager.isHidSupported.collectAsStateWithLifecycle()

    val isConnectedAnywhere = status == ConnectionStatus.CONNECTED || hidConnectedDevice != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF131D31), CyberDarkBg),
                    radius = 1200f
                )
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- TOP CONTROL BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F1728).copy(alpha = 0.85f))
                    .border(1.dp, Color(0xFF1E2D48), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Pill (Wi-Fi or Bluetooth HID)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            when {
                                isConnectedAnywhere -> NeonGreen.copy(alpha = 0.2f)
                                status == ConnectionStatus.CONNECTING -> NeonYellow.copy(alpha = 0.2f)
                                else -> Color(0xFF202A3D)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isConnectedAnywhere -> NeonGreen
                                status == ConnectionStatus.CONNECTING -> NeonYellow
                                else -> Color(0xFF384663)
                            },
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { showConnectDialog = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isConnectedAnywhere -> NeonGreen
                                    status == ConnectionStatus.CONNECTING -> NeonYellow
                                    else -> NeonRed
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (hidConnectedDevice != null || connType == ConnectionType.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isConnectedAnywhere) NeonGreen else Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when {
                            hidConnectedDevice != null -> "HID: ${try { hidConnectedDevice?.name ?: "TV" } catch (e: SecurityException) { "TV" }}"
                            status == ConnectionStatus.CONNECTED -> "$hostName (${pingMs}ms)"
                            status == ConnectionStatus.CONNECTING -> "Connecting..."
                            isHidRegistered -> "HID Ready • Connect TV"
                            else -> "Connect to TV"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Middle: Mode Switcher [🎮 Gamepad] / [📺 TV Remote]
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF162238))
                        .border(1.dp, Color(0xFF233554), RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeTabItem(
                        icon = Icons.Default.Gamepad,
                        label = "Gamepad",
                        isSelected = currentControlMode == PhoneControlMode.GAMEPAD,
                        onClick = { currentControlMode = PhoneControlMode.GAMEPAD }
                    )
                    ModeTabItem(
                        icon = Icons.Default.SettingsRemote,
                        label = "TV Remote",
                        isSelected = currentControlMode == PhoneControlMode.TV_REMOTE,
                        onClick = { currentControlMode = PhoneControlMode.TV_REMOTE }
                    )
                }

                // Right Controls: Sub-options or TV Mode button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentControlMode == PhoneControlMode.GAMEPAD) {
                        // Motion Steer Toggle
                        Button(
                            onClick = {
                                motionEnabled = !motionEnabled
                                clientEngine.setMotionControlsEnabled(motionEnabled)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (motionEnabled) NeonCyan.copy(alpha = 0.3f) else Color(0xFF1B273D)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (motionEnabled) "📳 Tilt: ON" else "📳 Tilt: OFF",
                                color = if (motionEnabled) NeonCyan else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Stick / Dpad Toggle
                        Button(
                            onClick = { useDpadInsteadOfStick = !useDpadInsteadOfStick },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B273D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (useDpadInsteadOfStick) "🎛️ D-Pad" else "🕹️ Stick",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Switch to TV Console Mode
                    Button(
                        onClick = onSwitchToTvMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243656)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("TV Mode", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            // --- BODY VIEW: GAMEPAD OR TV REMOTE ---
            when (currentControlMode) {
                PhoneControlMode.TV_REMOTE -> {
                    TvRemotePad(
                        clientEngine = clientEngine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                PhoneControlMode.GAMEPAD -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // --- SHOULDER BUTTONS ROW ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left Shoulders: L2 & L1
                            Row {
                                ShoulderButton(button = GameButton.L2) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.L2, pressed)
                                    clientEngine.onTriggersMove(if (pressed) 1f else 0f, 0f)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                ShoulderButton(button = GameButton.L1) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.L1, pressed)
                                }
                            }

                            // Center Auxiliary Buttons: SELECT, MENU, START
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AuxButton(label = "SELECT") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.SELECT, pressed)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                AuxButton(label = "MENU") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.MENU, pressed)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                AuxButton(label = "START") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.START, pressed)
                                }
                            }

                            // Right Shoulders: R1 & R2
                            Row {
                                ShoulderButton(button = GameButton.R1) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.R1, pressed)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                ShoulderButton(button = GameButton.R2) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.R2, pressed)
                                    clientEngine.onTriggersMove(0f, if (pressed) 1f else 0f)
                                }
                            }
                        }

                        // --- MAIN CONTROLLERS ROW (Stick/D-Pad on left, Action Buttons on right) ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Controller: Virtual Joystick OR D-Pad
                            Box(
                                modifier = Modifier
                                    .size(190.dp)
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (useDpadInsteadOfStick) {
                                    VirtualDpad(size = 170.dp) { btn, pressed ->
                                        clientEngine.onButtonEvent(btn, pressed)
                                        when (btn) {
                                            GameButton.LEFT -> clientEngine.onStickMove(if (pressed) -1f else 0f, 0f)
                                            GameButton.RIGHT -> clientEngine.onStickMove(if (pressed) 1f else 0f, 0f)
                                            GameButton.UP -> clientEngine.onStickMove(0f, if (pressed) -1f else 0f)
                                            GameButton.DOWN -> clientEngine.onStickMove(0f, if (pressed) 1f else 0f)
                                            else -> {}
                                        }
                                    }
                                } else {
                                    VirtualJoystick(size = 180.dp) { x, y ->
                                        clientEngine.onStickMove(x, y)
                                    }
                                }
                            }

                            // Center Console Branding / Logo
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "WIRELESS GAMEPAD",
                                    color = NeonCyan.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, NeonCyan, Color.Transparent)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isConnectedAnywhere) "CONNECTED TO TV" else "READY TO PAIR (BLUETOOTH / WI-FI)",
                                    color = if (isConnectedAnywhere) NeonGreen.copy(alpha = 0.8f) else Color.Gray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Right Action Diamond: A, B, X, Y
                            Box(
                                modifier = Modifier
                                    .size(190.dp)
                                    .padding(end = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ActionButtonsDiamond(size = 170.dp) { btn, pressed ->
                                    clientEngine.onButtonEvent(btn, pressed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConnectDialog) {
        ConnectionDialog(
            clientEngine = clientEngine,
            onDismiss = { showConnectDialog = false },
            onConnectWifi = { ip, port, name ->
                clientEngine.connectWifi(ip, port, name)
            },
            onConnectBt = { device ->
                clientEngine.connectBluetooth(device)
            }
        )
    }
}

@Composable
private fun ModeTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) NeonCyan else Color.Gray, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
