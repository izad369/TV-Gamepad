package com.example.ui.gamepad

import android.view.MotionEvent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvRemoteKey
import com.example.network.PhoneClientEngine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed

/**
 * TV Remote controller view on the phone to control Android TV navigation,
 * volume, power, media playback, channel switching, and system menus.
 */
@Composable
fun TvRemotePad(
    clientEngine: PhoneClientEngine,
    modifier: Modifier = Modifier
) {
    var showNumPad by remember { mutableStateOf(false) }
    var isTouchpadMode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sleek Remote Body Frame
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .border(
                    1.5.dp,
                    Brush.verticalGradient(listOf(NeonCyan.copy(alpha = 0.5f), Color(0xFF1E293B))),
                    RoundedCornerShape(32.dp)
                )
                .shadow(16.dp, RoundedCornerShape(32.dp)),
            color = Color(0xFF0C1322).copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Top Row: Power, Input Source, Mute, Menu ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteIconButton(
                        icon = Icons.Default.PowerSettingsNew,
                        key = TvRemoteKey.POWER,
                        accentColor = NeonRed,
                        tooltip = "پاور",
                        tag = "remote_btn_power",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemoteIconButton(
                        icon = Icons.Default.Input,
                        key = TvRemoteKey.INPUT_SOURCE,
                        accentColor = NeonOrange,
                        tooltip = "ورودی",
                        tag = "remote_btn_input",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemoteIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeMute,
                        key = TvRemoteKey.MUTE,
                        accentColor = Color.LightGray,
                        tooltip = "بی‌صدا",
                        tag = "remote_btn_mute",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemoteIconButton(
                        icon = Icons.Default.Menu,
                        key = TvRemoteKey.MENU,
                        accentColor = NeonCyan,
                        tooltip = "منو",
                        tag = "remote_btn_menu",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )
                }

                // Mode switch pills (D-Pad vs Touchpad, and Numpad)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isTouchpadMode) NeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B))
                            .border(1.dp, if (!isTouchpadMode) NeonCyan else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { isTouchpadMode = false }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("کلیدهای جهت‌نما", color = if (!isTouchpadMode) NeonCyan else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isTouchpadMode) NeonCyan.copy(alpha = 0.25f) else Color(0xFF1E293B))
                            .border(1.dp, if (isTouchpadMode) NeonCyan else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { isTouchpadMode = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = if (isTouchpadMode) NeonCyan else Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پد لمسی ماوس", color = if (isTouchpadMode) NeonCyan else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (showNumPad) NeonOrange.copy(alpha = 0.25f) else Color(0xFF1E293B))
                            .border(1.dp, if (showNumPad) NeonOrange else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable { showNumPad = !showNumPad }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Dialpad, contentDescription = null, tint = if (showNumPad) NeonOrange else Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اعداد", color = if (showNumPad) NeonOrange else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                // Center Navigation: D-Pad OR Touchpad
                if (!isTouchpadMode) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF1E293B), Color(0xFF0B1120))
                                )
                            )
                            .border(2.dp, Color(0xFF334155), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // UP
                        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)) {
                            RemoteNavSlice(
                                label = "▲",
                                key = TvRemoteKey.UP,
                                tag = "remote_nav_up",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }

                        // DOWN
                        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) {
                            RemoteNavSlice(
                                label = "▼",
                                key = TvRemoteKey.DOWN,
                                tag = "remote_nav_down",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }

                        // LEFT
                        Box(modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)) {
                            RemoteNavSlice(
                                label = "◀",
                                key = TvRemoteKey.LEFT,
                                tag = "remote_nav_left",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }

                        // RIGHT
                        Box(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)) {
                            RemoteNavSlice(
                                label = "▶",
                                key = TvRemoteKey.RIGHT,
                                tag = "remote_nav_right",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }

                        // Central OK button
                        RemoteOkButton(
                            key = TvRemoteKey.OK,
                            tag = "remote_nav_ok",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                    }
                } else {
                    // Touchpad Area
                    RemoteTouchpad(
                        clientEngine = clientEngine,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }

                // --- CRITICAL ROW: BACK & HOME BUTTONS ---
                Row(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemotePillButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        text = "بازگشت (Back)",
                        key = TvRemoteKey.BACK,
                        accentColor = NeonOrange,
                        tag = "remote_btn_back",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemotePillButton(
                        icon = Icons.Default.Home,
                        text = "خانه (Home)",
                        key = TvRemoteKey.HOME,
                        accentColor = NeonGreen,
                        tag = "remote_btn_home",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )
                }

                // --- Volume and Channel Controls Row ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF131F33))
                        .border(1.dp, Color(0xFF233554), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("صدا (VOL)", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RemoteIconButton(
                                icon = Icons.AutoMirrored.Filled.VolumeDown,
                                key = TvRemoteKey.VOL_DOWN,
                                accentColor = NeonCyan,
                                tooltip = "کاهش صدا",
                                tag = "remote_btn_voldown",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                            RemoteIconButton(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                key = TvRemoteKey.VOL_UP,
                                accentColor = NeonCyan,
                                tooltip = "افزایش صدا",
                                tag = "remote_btn_volup",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }
                    }

                    // Channel Section
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("کانال (CH)", color = NeonOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RemoteIconButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                key = TvRemoteKey.CH_DOWN,
                                accentColor = NeonOrange,
                                tooltip = "کانال قبل",
                                tag = "remote_btn_chdown",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                            RemoteIconButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                key = TvRemoteKey.CH_UP,
                                accentColor = NeonOrange,
                                tooltip = "کانال بعد",
                                tag = "remote_btn_chup",
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }
                    }
                }

                // --- Media Controls Row ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteIconButton(
                        icon = Icons.Default.FastRewind,
                        key = TvRemoteKey.REWIND,
                        accentColor = Color.White,
                        tooltip = "عقب",
                        tag = "remote_btn_rewind",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemoteIconButton(
                        icon = Icons.Default.PlayArrow,
                        key = TvRemoteKey.PLAY_PAUSE,
                        accentColor = NeonGreen,
                        tooltip = "پخش/توقف",
                        tag = "remote_btn_play",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemoteIconButton(
                        icon = Icons.Default.FastForward,
                        key = TvRemoteKey.FAST_FORWARD,
                        accentColor = Color.White,
                        tooltip = "جلو",
                        tag = "remote_btn_ffwd",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )
                }

                // Optional Numeric Keypad Box
                if (showNumPad) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF233554))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("صفحه‌کلید شماره کانال", color = Color.Gray, fontSize = 11.sp)
                            val numRows = listOf(
                                listOf(TvRemoteKey.NUM_1 to "1", TvRemoteKey.NUM_2 to "2", TvRemoteKey.NUM_3 to "3"),
                                listOf(TvRemoteKey.NUM_4 to "4", TvRemoteKey.NUM_5 to "5", TvRemoteKey.NUM_6 to "6"),
                                listOf(TvRemoteKey.NUM_7 to "7", TvRemoteKey.NUM_8 to "8", TvRemoteKey.NUM_9 to "9"),
                                listOf(TvRemoteKey.OK to "OK", TvRemoteKey.NUM_0 to "0", TvRemoteKey.BACK to "↩")
                            )
                            numRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    row.forEach { (key, label) ->
                                        RemoteNumKey(
                                            label = label,
                                            key = key,
                                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                                        )
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoteTouchpad(
    clientEngine: PhoneClientEngine,
    modifier: Modifier = Modifier
) {
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }
    var isTouching by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF131E34))
            .border(1.5.dp, if (isTouching) NeonCyan else Color(0xFF243656), RoundedCornerShape(20.dp))
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastX = motionEvent.x
                        lastY = motionEvent.y
                        isTouching = true
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = motionEvent.x - lastX
                        val dy = motionEvent.y - lastY
                        lastX = motionEvent.x
                        lastY = motionEvent.y

                        // Send simulated directional or mouse move
                        if (kotlin.math.abs(dx) > 30f || kotlin.math.abs(dy) > 30f) {
                            if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                                val key = if (dx > 0) TvRemoteKey.RIGHT else TvRemoteKey.LEFT
                                clientEngine.onRemoteKeyEvent(key, true)
                                clientEngine.onRemoteKeyEvent(key, false)
                            } else {
                                val key = if (dy > 0) TvRemoteKey.DOWN else TvRemoteKey.UP
                                clientEngine.onRemoteKeyEvent(key, true)
                                clientEngine.onRemoteKeyEvent(key, false)
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        isTouching = false
                        // Click tap = OK
                        clientEngine.onRemoteKeyEvent(TvRemoteKey.OK, true)
                        clientEngine.onRemoteKeyEvent(TvRemoteKey.OK, false)
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        isTouching = false
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = if (isTouching) NeonCyan else Color.Gray, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "انگشت خود را برای جابه‌جایی بکشید\nبرای انتخاب (OK) ضربه بزنید",
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoteIconButton(
    icon: ImageVector,
    key: TvRemoteKey,
    accentColor: Color,
    tooltip: String,
    tag: String,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isPressed) accentColor.copy(alpha = 0.35f) else Color(0xFF1E293B)
                )
                .border(
                    1.5.dp,
                    if (isPressed) accentColor else Color(0xFF334155),
                    CircleShape
                )
                .pointerInteropFilter { motionEvent ->
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            isPressed = true
                            onKey(key, true)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            isPressed = false
                            onKey(key, false)
                            true
                        }
                        else -> false
                    }
                }
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltip,
                tint = if (isPressed) accentColor else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = tooltip, color = Color.Gray, fontSize = 10.sp)
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemotePillButton(
    icon: ImageVector,
    text: String,
    key: TvRemoteKey,
    accentColor: Color,
    tag: String,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isPressed) accentColor.copy(alpha = 0.35f) else Color(0xFF1E293B)
            )
            .border(
                1.5.dp,
                if (isPressed) accentColor else Color(0xFF334155),
                RoundedCornerShape(20.dp)
            )
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onKey(key, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onKey(key, false)
                        true
                    }
                    else -> false
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isPressed) accentColor else Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (isPressed) accentColor else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoteNavSlice(
    label: String,
    key: TvRemoteKey,
    tag: String,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isPressed) NeonCyan.copy(alpha = 0.4f) else Color.Transparent)
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onKey(key, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onKey(key, false)
                        true
                    }
                    else -> false
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) NeonCyan else Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoteOkButton(
    key: TvRemoteKey,
    tag: String,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (isPressed) NeonCyan.copy(alpha = 0.5f) else Color(0xFF25334D)
            )
            .border(
                2.dp,
                if (isPressed) NeonCyan else Color(0xFF475569),
                CircleShape
            )
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onKey(key, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onKey(key, false)
                        true
                    }
                    else -> false
                }
            }
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "OK",
            color = if (isPressed) NeonCyan else Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoteNumKey(
    label: String,
    key: TvRemoteKey,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isPressed) NeonCyan.copy(alpha = 0.4f) else Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
            .pointerInteropFilter { motionEvent ->
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isPressed = true
                        onKey(key, true)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        isPressed = false
                        onKey(key, false)
                        true
                    }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) NeonCyan else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
