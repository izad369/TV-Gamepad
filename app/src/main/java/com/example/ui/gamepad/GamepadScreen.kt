package com.example.ui.gamepad

import android.content.Context
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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionStatus
import com.example.model.ConnectionType
import com.example.model.GameButton
import com.example.network.HttpFileServer
import com.example.network.PhoneClientEngine
import com.example.ui.filetransfer.FileTransferScreen
import com.example.ui.onboarding.OnboardingTutorialDialog
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow

enum class PhoneControlTab {
    GAMEPAD,
    TV_REMOTE,
    FILE_TRANSFER
}

@Composable
fun GamepadScreen(
    clientEngine: PhoneClientEngine,
    httpFileServer: HttpFileServer? = null,
    onSwitchToTvMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("gamepad_tv_prefs", Context.MODE_PRIVATE) }

    var showTutorialDialog by remember {
        mutableStateOf(sharedPrefs.getBoolean("has_seen_tutorial", false).not())
    }
    var showConnectDialog by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(PhoneControlTab.GAMEPAD) }

    // Gamepad customization toggles
    var useDpadInsteadOfLeftStick by remember { mutableStateOf(false) }
    var enableDualSticks by remember { mutableStateOf(true) } // Left stick + Right stick
    var enableSecondaryDpad by remember { mutableStateOf(false) } // Second D-pad on right
    var showSixButtonsArcade by remember { mutableStateOf(true) } // X, Y, Z + A, B
    var motionEnabled by remember { mutableStateOf(false) }

    val status by clientEngine.connectionStatus.collectAsStateWithLifecycle()
    val connType by clientEngine.connectionType.collectAsStateWithLifecycle()
    val hostName by clientEngine.connectedHostName.collectAsStateWithLifecycle()
    val pingMs by clientEngine.pingMs.collectAsStateWithLifecycle()

    val isHidRegistered by clientEngine.hidManager.isRegistered.collectAsStateWithLifecycle()
    val hidConnectedDevice by clientEngine.hidManager.connectedDevice.collectAsStateWithLifecycle()

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
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================================
            // 1. TOP HEADER & NAVIGATION BAR (Persian UI, status indicator, mode tabs)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1728).copy(alpha = 0.9f))
                    .border(1.dp, Color(0xFF1E2D48), RoundedCornerShape(14.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Pill
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
                            hidConnectedDevice != null -> "بلوتوث: ${try { hidConnectedDevice?.name ?: "تلویزیون" } catch (e: SecurityException) { "تلویزیون" }}"
                            status == ConnectionStatus.CONNECTED -> "وای‌فای: $hostName (${pingMs}ms)"
                            status == ConnectionStatus.CONNECTING -> "در حال اتصال..."
                            isHidRegistered -> "بلوتوث آماده • اتصال به TV"
                            else -> "اتصال به تلویزیون"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Middle: Mode Switcher Tabs
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF162238))
                        .border(1.dp, Color(0xFF233554), RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersianModeTabItem(
                        icon = Icons.Default.Gamepad,
                        label = "دسته بازی",
                        isSelected = currentTab == PhoneControlTab.GAMEPAD,
                        onClick = { currentTab = PhoneControlTab.GAMEPAD }
                    )
                    PersianModeTabItem(
                        icon = Icons.Default.SettingsRemote,
                        label = "کنترل تلویزیون",
                        isSelected = currentTab == PhoneControlTab.TV_REMOTE,
                        onClick = { currentTab = PhoneControlTab.TV_REMOTE }
                    )
                    PersianModeTabItem(
                        icon = Icons.Default.Send,
                        label = "ارسال فایل",
                        isSelected = currentTab == PhoneControlTab.FILE_TRANSFER,
                        onClick = { currentTab = PhoneControlTab.FILE_TRANSFER }
                    )
                }

                // Right Utility Controls: Help, Tilt, and TV Console Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentTab == PhoneControlTab.GAMEPAD) {
                        // Tilt Gyro Toggle
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
                                text = if (motionEnabled) "📳 حسگر فعال" else "📳 حسگر",
                                color = if (motionEnabled) NeonCyan else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Controls layout toggle (XYZ / Sticks)
                        Button(
                            onClick = {
                                showSixButtonsArcade = !showSixButtonsArcade
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B273D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (showSixButtonsArcade) "🎮 حالت XYZ" else "🎮 حالت ۴ دکمه",
                                color = NeonPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Help & Tutorial Button
                    Button(
                        onClick = { showTutorialDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D48)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "آموزش", tint = NeonYellow, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("آموزش", color = Color.White, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Switch to TV Mode
                    Button(
                        onClick = onSwitchToTvMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF243656)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حالت TV", color = Color.White, fontSize = 10.sp)
                    }
                }
            }

            // =========================================================================
            // 2. MAIN ACTIVE TAB CONTENT
            // =========================================================================
            when (currentTab) {
                PhoneControlTab.TV_REMOTE -> {
                    TvRemotePad(
                        clientEngine = clientEngine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                PhoneControlTab.FILE_TRANSFER -> {
                    val scope = rememberCoroutineScope()
                    FileTransferScreen(
                        fileServer = httpFileServer ?: remember { HttpFileServer(context, scope) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                PhoneControlTab.GAMEPAD -> {
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
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
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

                            // Center Auxiliary Buttons: SELECT, HOME, MENU, START
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AuxButton(label = "SELECT") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.SELECT, pressed)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                AuxButton(label = "HOME") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.HOME, pressed)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                AuxButton(label = "MENU") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.MENU, pressed)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
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

                        // --- MAIN CONTROLS ROW: DUAL JOYSTICKS / D-PADS & ACTION CLUSTERS ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // LEFT SIDE CONTROLLER:
                            // Dual Left Options: Left Joystick + Primary D-Pad
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Primary Left Stick
                                Box(
                                    modifier = Modifier
                                        .size(175.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VirtualJoystick(
                                        size = 170.dp,
                                        accentColor = NeonCyan,
                                        tag = "virtual_joystick_left"
                                    ) { x, y ->
                                        clientEngine.onStickMove(x, y)
                                    }
                                }

                                // Primary D-Pad (بالا، پایین، چپ، راست اول)
                                Box(
                                    modifier = Modifier.size(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VirtualDpad(
                                        size = 145.dp,
                                        accentColor = NeonCyan,
                                        tag = "virtual_dpad_left"
                                    ) { btn, pressed ->
                                        clientEngine.onButtonEvent(btn, pressed)
                                    }
                                }
                            }

                            // CENTER STATUS BADGE
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "دسته بازی بیسیم",
                                    color = NeonCyan.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(2.dp)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.Transparent, NeonCyan, Color.Transparent)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isConnectedAnywhere) "متصل به تلویزیون" else "آماده اتصال به تلویزیون",
                                    color = if (isConnectedAnywhere) NeonGreen else Color.Gray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // RIGHT SIDE CONTROLLER:
                            // Right Joystick (دسته دوم) + Action Cluster with X, Y, Z, A, B + Secondary D-Pad
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Action Cluster: X, Y, Z, A, B ("و xyz")
                                Box(
                                    modifier = Modifier.size(175.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (showSixButtonsArcade) {
                                        ActionButtonsWithZ(size = 175.dp) { btn, pressed ->
                                            clientEngine.onButtonEvent(btn, pressed)
                                        }
                                    } else {
                                        ActionButtonsDiamond(size = 165.dp) { btn, pressed ->
                                            clientEngine.onButtonEvent(btn, pressed)
                                        }
                                    }
                                }

                                // Secondary Controller Stick (دسته کنترل دوم برای دوربین / هدف‌گیری)
                                Box(
                                    modifier = Modifier.size(175.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    VirtualJoystick(
                                        size = 170.dp,
                                        accentColor = NeonPurple,
                                        tag = "virtual_joystick_right"
                                    ) { rx, ry ->
                                        clientEngine.onRightStickMove(rx, ry)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Connection Dialog (Bluetooth HID or Wi-Fi)
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

    // Onboarding Tutorial Dialog (Shown automatically on first launch, or when clicking Help)
    if (showTutorialDialog) {
        OnboardingTutorialDialog(
            onDismiss = {
                showTutorialDialog = false
                sharedPrefs.edit().putBoolean("has_seen_tutorial", true).apply()
            }
        )
    }
}

@Composable
private fun PersianModeTabItem(
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
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) NeonCyan else Color.Gray,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
