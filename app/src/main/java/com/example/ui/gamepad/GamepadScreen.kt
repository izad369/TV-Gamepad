package com.example.ui.gamepad

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

enum class GamepadLayoutMode {
    ALL_IN_ONE, // Dual Sticks + D-pad + 6 Buttons (Compact Adaptive)
    DUAL_STICKS, // Modern Console: Left Stick + Right Stick + ABXY
    CLASSIC_ARCADE // Retro Arcade: Large D-Pad + 6 Action Buttons (XYZ + AB)
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

    // Gamepad layout mode
    var layoutMode by remember { mutableStateOf(GamepadLayoutMode.ALL_IN_ONE) }
    var leftControlIsStick by remember { mutableStateOf(true) } // For quick toggles
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
                    colors = listOf(Color(0xFF131E33), CyberDarkBg),
                    radius = 1200f
                )
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================================
            // 1. TOP HEADER & NAVIGATION BAR (Persian UI, High-contrast, Mode tabs)
            // =========================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1525).copy(alpha = 0.95f))
                    .border(1.5.dp, Color(0xFF223554), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Status Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            when {
                                isConnectedAnywhere -> NeonGreen.copy(alpha = 0.25f)
                                status == ConnectionStatus.CONNECTING -> NeonYellow.copy(alpha = 0.25f)
                                else -> Color(0xFF1E2A3E)
                            }
                        )
                        .border(
                            1.5.dp,
                            when {
                                isConnectedAnywhere -> NeonGreen
                                status == ConnectionStatus.CONNECTING -> NeonYellow
                                else -> Color(0xFF384D73)
                            },
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { showConnectDialog = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
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
                        tint = if (isConnectedAnywhere) NeonGreen else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = when {
                            hidConnectedDevice != null -> "بلوتوث: ${try { hidConnectedDevice?.name ?: "تلویزیون" } catch (e: SecurityException) { "تلویزیون" }}"
                            status == ConnectionStatus.CONNECTED -> "وای‌فای: $hostName (${pingMs}ms)"
                            status == ConnectionStatus.CONNECTING -> "در حال اتصال..."
                            isHidRegistered -> "بلوتوث آماده • لمس برای اتصال"
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
                        .background(Color(0xFF152238))
                        .border(1.dp, Color(0xFF283D61), RoundedCornerShape(10.dp))
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

                // Right Utility Controls: Layout Toggle, Tilt, Help, TV Console Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentTab == PhoneControlTab.GAMEPAD) {
                        // Layout Mode Switcher (All-In-One, Dual-Sticks, Classic Arcade)
                        Button(
                            onClick = {
                                layoutMode = when (layoutMode) {
                                    GamepadLayoutMode.ALL_IN_ONE -> GamepadLayoutMode.DUAL_STICKS
                                    GamepadLayoutMode.DUAL_STICKS -> GamepadLayoutMode.CLASSIC_ARCADE
                                    GamepadLayoutMode.CLASSIC_ARCADE -> GamepadLayoutMode.ALL_IN_ONE
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2B44)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = when (layoutMode) {
                                    GamepadLayoutMode.ALL_IN_ONE -> "طرح: ترکیبی کامل"
                                    GamepadLayoutMode.DUAL_STICKS -> "طرح: دو آنالوگ"
                                    GamepadLayoutMode.CLASSIC_ARCADE -> "طرح: ۴ جهته و ۶ دکمه"
                                },
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))

                        // Tilt Gyro Toggle
                        Button(
                            onClick = {
                                motionEnabled = !motionEnabled
                                clientEngine.setMotionControlsEnabled(motionEnabled)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (motionEnabled) NeonPurple.copy(alpha = 0.4f) else Color(0xFF1B2B44)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = if (motionEnabled) "📳 حسگر روشن" else "📳 حسگر",
                                color = if (motionEnabled) Color.White else Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    // Help & Tutorial Button
                    Button(
                        onClick = { showTutorialDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2B44)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "آموزش", tint = NeonYellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("آموزش", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Switch to TV Mode
                    Button(
                        onClick = onSwitchToTvMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263B5D)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("صفحه TV", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        // --- SHOULDER BUTTONS ROW (L1, L2, AUX, R1, R2) ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Shoulders: L2 & L1
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ShoulderButton(button = GameButton.L2) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.L2, pressed)
                                    clientEngine.onTriggersMove(if (pressed) 1f else 0f, 0f)
                                }
                                ShoulderButton(button = GameButton.L1) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.L1, pressed)
                                }
                            }

                            // Center Auxiliary Buttons: SELECT, HOME, MENU, START
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AuxButton(label = "SELECT") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.SELECT, pressed)
                                }
                                AuxButton(label = "HOME") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.HOME, pressed)
                                }
                                AuxButton(label = "MENU") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.MENU, pressed)
                                }
                                AuxButton(label = "START") { pressed ->
                                    clientEngine.onButtonEvent(GameButton.START, pressed)
                                }
                            }

                            // Right Shoulders: R1 & R2
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                ShoulderButton(button = GameButton.R1) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.R1, pressed)
                                }
                                ShoulderButton(button = GameButton.R2) { pressed ->
                                    clientEngine.onButtonEvent(GameButton.R2, pressed)
                                    clientEngine.onTriggersMove(0f, if (pressed) 1f else 0f)
                                }
                            }
                        }

                        // --- MAIN CONTROLS: ADAPTIVE, NON-OVERLAPPING LAYOUT ---
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val availWidth = maxWidth
                            val availHeight = maxHeight

                            // Adapt sizes to available space so components NEVER overlap
                            val joystickSize = (availHeight * 0.82f).coerceIn(120.dp, 160.dp)
                            val dpadSize = (availHeight * 0.76f).coerceIn(115.dp, 145.dp)
                            val actionSize = (availHeight * 0.82f).coerceIn(125.dp, 160.dp)

                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // =========================================================
                                // LEFT SIDE: Movement (Joystick and/or D-pad)
                                // =========================================================
                                when (layoutMode) {
                                    GamepadLayoutMode.ALL_IN_ONE -> {
                                        // If space is generous (>= 720dp), show both side-by-side; otherwise show primary with a quick toggle
                                        if (availWidth >= 700.dp) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                VirtualJoystick(
                                                    size = 135.dp,
                                                    accentColor = NeonCyan,
                                                    tag = "virtual_joystick_left"
                                                ) { x, y -> clientEngine.onStickMove(x, y) }

                                                VirtualDpad(
                                                    size = 125.dp,
                                                    accentColor = NeonCyan,
                                                    tag = "virtual_dpad_left"
                                                ) { btn, pressed -> clientEngine.onButtonEvent(btn, pressed) }
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (leftControlIsStick) {
                                                    VirtualJoystick(
                                                        size = joystickSize,
                                                        accentColor = NeonCyan,
                                                        tag = "virtual_joystick_left"
                                                    ) { x, y -> clientEngine.onStickMove(x, y) }
                                                } else {
                                                    VirtualDpad(
                                                        size = dpadSize,
                                                        accentColor = NeonCyan,
                                                        tag = "virtual_dpad_left"
                                                    ) { btn, pressed -> clientEngine.onButtonEvent(btn, pressed) }
                                                }

                                                // Switch toggle button
                                                IconButtonSmall(
                                                    icon = Icons.Default.SwapHoriz,
                                                    tooltip = "تغییر به ${if (leftControlIsStick) "چهارجهته" else "جوی‌استیک"}",
                                                    onClick = { leftControlIsStick = !leftControlIsStick }
                                                )
                                            }
                                        }
                                    }
                                    GamepadLayoutMode.DUAL_STICKS -> {
                                        VirtualJoystick(
                                            size = joystickSize,
                                            accentColor = NeonCyan,
                                            tag = "virtual_joystick_left"
                                        ) { x, y -> clientEngine.onStickMove(x, y) }
                                    }
                                    GamepadLayoutMode.CLASSIC_ARCADE -> {
                                        VirtualDpad(
                                            size = dpadSize * 1.15f,
                                            accentColor = NeonCyan,
                                            tag = "virtual_dpad_left"
                                        ) { btn, pressed -> clientEngine.onButtonEvent(btn, pressed) }
                                    }
                                }

                                // =========================================================
                                // CENTER STATUS BADGE
                                // =========================================================
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "گیم‌پد بیسیم",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(2.dp)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color.Transparent, NeonCyan, Color.Transparent)
                                                )
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isConnectedAnywhere) "آماده بازی 🎮" else "در انتظار اتصال 📡",
                                        color = if (isConnectedAnywhere) NeonGreen else Color(0xFFFBBF24),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // =========================================================
                                // RIGHT SIDE: Actions & Camera/Aim Joystick
                                // =========================================================
                                when (layoutMode) {
                                    GamepadLayoutMode.ALL_IN_ONE -> {
                                        if (availWidth >= 700.dp) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                ActionButtonsWithZ(size = 140.dp) { btn, pressed ->
                                                    clientEngine.onButtonEvent(btn, pressed)
                                                }

                                                VirtualJoystick(
                                                    size = 135.dp,
                                                    accentColor = NeonPurple,
                                                    tag = "virtual_joystick_right"
                                                ) { rx, ry -> clientEngine.onRightStickMove(rx, ry) }
                                            }
                                        } else {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                ActionButtonsWithZ(size = actionSize) { btn, pressed ->
                                                    clientEngine.onButtonEvent(btn, pressed)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                VirtualJoystick(
                                                    size = (joystickSize * 0.88f).coerceAtLeast(105.dp),
                                                    accentColor = NeonPurple,
                                                    tag = "virtual_joystick_right"
                                                ) { rx, ry -> clientEngine.onRightStickMove(rx, ry) }
                                            }
                                        }
                                    }
                                    GamepadLayoutMode.DUAL_STICKS -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ActionButtonsDiamond(size = actionSize) { btn, pressed ->
                                                clientEngine.onButtonEvent(btn, pressed)
                                            }
                                            VirtualJoystick(
                                                size = joystickSize,
                                                accentColor = NeonPurple,
                                                tag = "virtual_joystick_right"
                                            ) { rx, ry -> clientEngine.onRightStickMove(rx, ry) }
                                        }
                                    }
                                    GamepadLayoutMode.CLASSIC_ARCADE -> {
                                        ActionButtonsWithZ(size = actionSize * 1.15f) { btn, pressed ->
                                            clientEngine.onButtonEvent(btn, pressed)
                                        }
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
            .background(if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) NeonCyan else Color(0xFFCBD5E1),
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IconButtonSmall(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0xFF1B2B44))
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = NeonCyan,
            modifier = Modifier.size(18.dp)
        )
    }
}
