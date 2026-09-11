package com.example.ui.gamepad

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvRemoteKey
import com.example.network.PhoneClientEngine
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed

/**
 * TV Remote controller view on the phone to control Android TV navigation,
 * volume, power, media playback, and system menus.
 */
@Composable
fun TvRemotePad(
    clientEngine: PhoneClientEngine,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sleek Remote Body Frame
        Surface(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .border(1.5.dp, Brush.verticalGradient(listOf(NeonCyan.copy(alpha = 0.5f), Color(0xFF1E293B))), RoundedCornerShape(32.dp))
                .shadow(16.dp, RoundedCornerShape(32.dp)),
            color = Color(0xFF0F172A).copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Row: Power & Mute & Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteIconButton(
                        icon = Icons.Default.PowerSettingsNew,
                        key = TvRemoteKey.POWER,
                        accentColor = NeonRed,
                        tag = "remote_btn_power",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    Text(
                        text = "TV REMOTE",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )

                    RemoteIconButton(
                        icon = Icons.Default.Menu,
                        key = TvRemoteKey.MENU,
                        accentColor = NeonCyan,
                        tag = "remote_btn_menu",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Center Navigation Wheel (DPAD & OK)
                Box(
                    modifier = Modifier
                        .size(210.dp)
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

                Spacer(modifier = Modifier.height(6.dp))

                // Navigation Row: Back & Home
                Row(
                    modifier = Modifier.fillMaxWidth(0.85f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemotePillButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        text = "BACK",
                        key = TvRemoteKey.BACK,
                        accentColor = NeonOrange,
                        tag = "remote_btn_back",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    RemotePillButton(
                        icon = Icons.Default.Home,
                        text = "HOME",
                        key = TvRemoteKey.HOME,
                        accentColor = NeonGreen,
                        tag = "remote_btn_home",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Media and Volume Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF131F33))
                        .border(1.dp, Color(0xFF233554), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Volume Down
                    RemoteIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        key = TvRemoteKey.VOL_DOWN,
                        accentColor = NeonCyan,
                        tag = "remote_btn_voldown",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    // Volume Mute
                    RemoteIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeMute,
                        key = TvRemoteKey.MUTE,
                        accentColor = Color.LightGray,
                        tag = "remote_btn_mute",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    // Volume Up
                    RemoteIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        key = TvRemoteKey.VOL_UP,
                        accentColor = NeonCyan,
                        tag = "remote_btn_volup",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    // Rewind
                    RemoteIconButton(
                        icon = Icons.Default.FastRewind,
                        key = TvRemoteKey.REWIND,
                        accentColor = Color.White,
                        tag = "remote_btn_rewind",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    // Play/Pause
                    RemoteIconButton(
                        icon = Icons.Default.PlayArrow,
                        key = TvRemoteKey.PLAY_PAUSE,
                        accentColor = NeonGreen,
                        tag = "remote_btn_play",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )

                    // Fast Forward
                    RemoteIconButton(
                        icon = Icons.Default.FastForward,
                        key = TvRemoteKey.FAST_FORWARD,
                        accentColor = Color.White,
                        tag = "remote_btn_ffwd",
                        onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RemoteIconButton(
    icon: ImageVector,
    key: TvRemoteKey,
    accentColor: Color,
    tag: String,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

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
            contentDescription = key.name,
            tint = if (isPressed) accentColor else Color.White,
            modifier = Modifier.size(22.dp)
        )
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isPressed) accentColor else Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = if (isPressed) accentColor else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
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
