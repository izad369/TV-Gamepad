package com.example.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ConnectionStatus
import com.example.network.TVServerEngine
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonYellow

enum class TvViewMode {
    DASHBOARD,
    DIAGNOSTICS_LAB,
    HID_GUIDE
}

/**
 * TV Console Receiver screen.
 * Optimized for 10-foot TV viewing and 100% operable via physical Android TV Remote (D-Pad).
 */
@Composable
fun TVConsoleScreen(
    serverEngine: TVServerEngine,
    onSwitchToPhoneMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentView by remember { mutableStateOf(TvViewMode.DASHBOARD) }

    val players by serverEngine.players.collectAsStateWithLifecycle()
    val serverStatus by serverEngine.serverStatus.collectAsStateWithLifecycle()
    val serverIp by serverEngine.serverIp.collectAsStateWithLifecycle()
    val serverPort by serverEngine.serverPort.collectAsStateWithLifecycle()
    val lastRemoteKey by serverEngine.lastRemoteKey.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        serverEngine.startServer()
    }

    when (currentView) {
        TvViewMode.DIAGNOSTICS_LAB -> {
            ControllerDiagnostics(
                players = players,
                onExit = { currentView = TvViewMode.DASHBOARD },
                modifier = modifier
            )
        }
        TvViewMode.HID_GUIDE -> {
            TvHidGuideView(
                onBack = { currentView = TvViewMode.DASHBOARD },
                modifier = modifier
            )
        }
        TvViewMode.DASHBOARD -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0D1627), CyberDarkBg)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // --- TOP STATUS BAR ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CyberCardBg)
                            .border(1.dp, CyberCardBorder, RoundedCornerShape(16.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TV Brand / Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Tv, contentDescription = "TV", tint = NeonCyan, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "ANDROID TV GAMEPAD RECEIVER",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Wi-Fi Server: $serverIp:$serverPort",
                                        color = NeonGreen,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bluetooth HID Ready",
                                        color = NeonCyan,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Connected Controllers Status & Switch Mode
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (players.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF241D12))
                                        .border(1.dp, NeonOrange, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonOrange))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Waiting for Phone...", color = NeonOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                players.forEach { p ->
                                    Row(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(NeonGreen.copy(alpha = 0.2f))
                                            .border(1.dp, NeonGreen, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.SportsEsports, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${p.name} (${p.connectionType})", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Switch to Phone Mode button (TV Remote Focusable)
                            TvFocusableButton(
                                text = "Phone Mode",
                                icon = Icons.Default.PhoneAndroid,
                                accentColor = NeonCyan,
                                tag = "btn_switch_phone_mode",
                                onClick = onSwitchToPhoneMode
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "WIRELESS CONTROLLER & REMOTE HUB",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Use your TV remote (D-Pad) to navigate, or control any TV game directly from your phone",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }

                        if (lastRemoteKey != null && lastRemoteKey?.second == true) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NeonCyan.copy(alpha = 0.2f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Last Remote Key: ", color = Color.White, fontSize = 12.sp)
                                Text(lastRemoteKey?.first?.label ?: "", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- 3 MAIN CARDS (Full TV Remote D-Pad Focusable) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card 1: Live Controller & Remote Diagnostics
                        TvRemoteCard(
                            title = "Controller Test Lab",
                            subtitle = "Real-time visual test of all gamepad buttons, joysticks, triggers, and remote keys.",
                            badge = "Live Tester",
                            icon = Icons.Default.Build,
                            accentColor = NeonCyan,
                            modifier = Modifier.weight(1f),
                            actionText = "Open Test Lab (Enter)",
                            tag = "card_test_lab",
                            onClick = { currentView = TvViewMode.DIAGNOSTICS_LAB }
                        )

                        // Card 2: Bluetooth HID Setup Guide for any installed TV Game
                        TvRemoteCard(
                            title = "Play Any TV Game (HID)",
                            subtitle = "How to pair phone over Bluetooth HID so it controls all installed Android TV games natively.",
                            badge = "Universal HID",
                            icon = Icons.Default.Gamepad,
                            accentColor = NeonGreen,
                            modifier = Modifier.weight(1f),
                            actionText = "View HID Pairing Guide",
                            tag = "card_hid_guide",
                            onClick = { currentView = TvViewMode.HID_GUIDE }
                        )

                        // Card 3: Wi-Fi & LAN Live Receiver
                        TvRemoteCard(
                            title = "Wi-Fi Companion Hub",
                            subtitle = "Zero-config companion receiver over local network with low-latency direct streaming.",
                            badge = "Network Stream",
                            icon = Icons.Default.Wifi,
                            accentColor = NeonYellow,
                            modifier = Modifier.weight(1f),
                            actionText = "Server Info",
                            tag = "card_wifi_hub",
                            onClick = { /* Informational */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- BOTTOM QUICK BAR ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F1728))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 راهنما: برنامه گوشی را باز کنید و از دسته بازی (Gamepad) یا ریموت تلویزیون (TV Remote) استفاده کنید.",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        Text(
                            text = "D-Pad Navigation: ⬆ ⬇ ⬅ ➡ + OK",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * TV Remote-friendly interactive card with high-visibility glowing focus ring.
 */
@Composable
private fun TvRemoteCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    actionText: String,
    tag: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else CyberCardBorder,
        label = "borderColor"
    )
    val animatedBgColor by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF1B2B48) else CyberCardBg,
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(animatedBgColor)
            .border(if (isFocused) 3.dp else 1.dp, animatedBorderColor, RoundedCornerShape(18.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(18.dp)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badge, color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    color = if (isFocused) Color.White else Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isFocused) accentColor else Color(0xFF1E293B))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionText,
                    color = if (isFocused) Color.Black else accentColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * TV Focusable Button for TV remote D-Pad navigation.
 */
@Composable
private fun TvFocusableButton(
    text: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) accentColor else Color(0xFF1E2D4A))
            .border(2.dp, if (isFocused) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isFocused) Color.Black else accentColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = if (isFocused) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Visual step-by-step guide for pairing phone over Bluetooth HID to control ANY installed game on TV.
 */
@Composable
private fun TvHidGuideView(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CyberDarkBg)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "راهنمای اتصال بلوتوث گیم‌پد سخت‌افزاری (HID)",
                        color = NeonGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "با استفاده از استاندارد بلوتوث HID، گوشی شما دقیقاً مانند یک دسته بازی فیزیکی به تمام بازی‌های تلویزیون متصل می‌شود.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }

                TvFocusableButton(
                    text = "بازگشت (Back)",
                    icon = Icons.Default.Tv,
                    accentColor = NeonCyan,
                    tag = "btn_back_hid_guide",
                    onClick = onBack
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Steps row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GuideStepCard(
                    stepNum = "۱",
                    title = "بلوتوث گوشی",
                    description = "در برنامه گوشی دکمه Connect را زده و زبانه Bluetooth را انتخاب کنید تا سرویس گیم‌پد فعال شود.",
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )

                GuideStepCard(
                    stepNum = "۲",
                    title = "تنظیمات تلویزیون",
                    description = "در تلویزیون وارد Settings (تنظیمات) شده، بخش Remotes & Accessories (کنترل‌ها و لوازم جانبی) و سپس Add accessory را بزنید.",
                    color = NeonYellow,
                    modifier = Modifier.weight(1f)
                )

                GuideStepCard(
                    stepNum = "۳",
                    title = "جفت‌سازی (Pair)",
                    description = "نام گوشی خود را در لیست جستجوی تلویزیون انتخاب کرده و تأیید کنید. تلویزیون آن را به عنوان Wireless Gamepad می‌شناسد.",
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )

                GuideStepCard(
                    stepNum = "۴",
                    title = "اجرای هر بازی دلخواه",
                    description = "اکنون هر بازی‌ای که روی تلویزیون نصب دارید (مانند Asphalt, Beach Buggy, Minecraft, RetroArch) را باز کرده و با گوشی بازی کنید!",
                    color = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131D31))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "✅ نیازی به نصب هیچ نرم‌افزار جانبی یا مپ کردن دکمه‌ها روی تلویزیون نیست! استاندارد HID در سطح هسته لینوکس و اندروید پشتیبانی می‌شود.",
                    color = NeonGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GuideStepCard(
    stepNum: String,
    title: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(CyberCardBg)
            .border(1.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f))
                    .border(1.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(stepNum, color = color, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }

            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(description, color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(color)
            )
        }
    }
}
