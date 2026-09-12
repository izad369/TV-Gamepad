package com.example.ui.gamepad

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TvRemoteKey
import com.example.network.PhoneClientEngine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow

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
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .border(
                    1.5.dp,
                    Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.5f))),
                    RoundedCornerShape(20.dp)
                ),
            color = Color(0xFF0C1424).copy(alpha = 0.95f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // =========================================================================
                // COLUMN 1 (LEFT): System Keys (Power, Home, Back, Menu, Source, Mute)
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(180.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "کنترل سیستم",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Row 1: Power & Source
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RemoteKeyBox(
                            icon = Icons.Default.PowerSettingsNew,
                            label = "خاموش/روشن",
                            key = TvRemoteKey.POWER,
                            accentColor = NeonRed,
                            tag = "remote_btn_power",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                        RemoteKeyBox(
                            icon = Icons.Default.Input,
                            label = "ورودی HDMI",
                            key = TvRemoteKey.INPUT_SOURCE,
                            accentColor = NeonYellow,
                            tag = "remote_btn_source",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                    }

                    // Row 2: Home & Back
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RemoteKeyBox(
                            icon = Icons.Default.Home,
                            label = "صفحه اصلی (Home)",
                            key = TvRemoteKey.HOME,
                            accentColor = NeonGreen,
                            tag = "remote_btn_home",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                        RemoteKeyBox(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "بازگشت (Back)",
                            key = TvRemoteKey.BACK,
                            accentColor = NeonCyan,
                            tag = "remote_btn_back",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                    }

                    // Row 3: Menu & Mute
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RemoteKeyBox(
                            icon = Icons.Default.Menu,
                            label = "منو تنظیمات",
                            key = TvRemoteKey.MENU,
                            accentColor = NeonOrange,
                            tag = "remote_btn_menu",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                        RemoteKeyBox(
                            icon = Icons.AutoMirrored.Filled.VolumeMute,
                            label = "قطع صدا",
                            key = TvRemoteKey.MUTE,
                            accentColor = Color.White,
                            tag = "remote_btn_mute",
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                    }
                }

                // =========================================================================
                // COLUMN 2 (CENTER): D-Pad & OK button OR Touchpad Mouse
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Mode Toggle: D-Pad vs Touchpad
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF16233B))
                            .border(1.dp, Color(0xFF2B3F63), RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { isTouchpadMode = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isTouchpadMode) NeonCyan.copy(alpha = 0.35f) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = if (!isTouchpadMode) NeonCyan else Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("کلیدهای ۴ جهته", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isTouchpadMode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isTouchpadMode) NeonPurple.copy(alpha = 0.35f) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Mouse, contentDescription = null, tint = if (isTouchpadMode) NeonPurple else Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ماوس‌پد تاچ", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isTouchpadMode) {
                        TvTouchpadView(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(180.dp),
                            onTap = {
                                clientEngine.onRemoteKeyEvent(TvRemoteKey.OK, true)
                                clientEngine.onRemoteKeyEvent(TvRemoteKey.OK, false)
                            },
                            onMoveDelta = { dx, dy ->
                                when {
                                    dx > 18 -> clientEngine.onRemoteKeyEvent(TvRemoteKey.RIGHT, true).also { clientEngine.onRemoteKeyEvent(TvRemoteKey.RIGHT, false) }
                                    dx < -18 -> clientEngine.onRemoteKeyEvent(TvRemoteKey.LEFT, true).also { clientEngine.onRemoteKeyEvent(TvRemoteKey.LEFT, false) }
                                    dy > 18 -> clientEngine.onRemoteKeyEvent(TvRemoteKey.DOWN, true).also { clientEngine.onRemoteKeyEvent(TvRemoteKey.DOWN, false) }
                                    dy < -18 -> clientEngine.onRemoteKeyEvent(TvRemoteKey.UP, true).also { clientEngine.onRemoteKeyEvent(TvRemoteKey.UP, false) }
                                }
                            }
                        )
                    } else {
                        // Circular Directional Pad with OK
                        TvDirectionalDisc(
                            size = 185.dp,
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                    }
                }

                // =========================================================================
                // COLUMN 3 (RIGHT): Volume, Channel, Playback & Numpad
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(200.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "صدا و پخش",
                        color = NeonPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Volume & Channel Pill Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Volume Pill (VOL+ and VOL-)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF152136))
                                .border(1.5.dp, Color(0xFF283D61), RoundedCornerShape(24.dp))
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RemotePillHalfButton(
                                icon = Icons.AutoMirrored.Filled.VolumeUp,
                                label = "صدا +",
                                key = TvRemoteKey.VOL_UP,
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                            Text("VOL", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            RemotePillHalfButton(
                                icon = Icons.AutoMirrored.Filled.VolumeDown,
                                label = "صدا -",
                                key = TvRemoteKey.VOL_DOWN,
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }

                        // Channel Pill (CH+ and CH-)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF152136))
                                .border(1.5.dp, Color(0xFF283D61), RoundedCornerShape(24.dp))
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RemotePillHalfButton(
                                icon = Icons.Default.KeyboardArrowUp,
                                label = "کانال +",
                                key = TvRemoteKey.CH_UP,
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                            Text("CH", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            RemotePillHalfButton(
                                icon = Icons.Default.KeyboardArrowDown,
                                label = "کانال -",
                                key = TvRemoteKey.CH_DOWN,
                                onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                            )
                        }
                    }

                    // Media Playback Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RemoteMiniIconButton(
                            icon = Icons.Default.FastRewind,
                            key = TvRemoteKey.REWIND,
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                        RemoteMiniIconButton(
                            icon = Icons.Default.PlayArrow,
                            key = TvRemoteKey.PLAY_PAUSE,
                            accent = NeonGreen,
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                        RemoteMiniIconButton(
                            icon = Icons.Default.FastForward,
                            key = TvRemoteKey.FAST_FORWARD,
                            onKey = { k, p -> clientEngine.onRemoteKeyEvent(k, p) }
                        )
                        RemoteMiniIconButton(
                            icon = Icons.Default.Dialpad,
                            key = TvRemoteKey.NUM_0,
                            accent = NeonYellow,
                            onClickCustom = { showNumPad = !showNumPad },
                            onKey = { _, _ -> }
                        )
                    }
                }
            }
        }

        // Numpad Overlay Dialog
        if (showNumPad) {
            TvNumpadOverlay(
                onClose = { showNumPad = false },
                onNumberKey = { key ->
                    clientEngine.onRemoteKeyEvent(key, true)
                    clientEngine.onRemoteKeyEvent(key, false)
                }
            )
        }
    }
}

@Composable
private fun RemoteKeyBox(
    icon: ImageVector,
    label: String,
    key: TvRemoteKey,
    accentColor: Color,
    tag: String,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 800f),
        label = "remote_btn_$tag"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .scale(scale)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isPressed) accentColor.copy(alpha = 0.5f) else Color(0xFF16233B)
                )
                .border(
                    1.5.dp,
                    if (isPressed) Color.White else accentColor.copy(alpha = 0.8f),
                    RoundedCornerShape(12.dp)
                )
                .pointerInput(key) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            onKey(key, true)
                            tryAwaitRelease()
                            isPressed = false
                            onKey(key, false)
                        }
                    )
                }
                .testTag(tag),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isPressed) Color.White else accentColor,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFFE2E8F0),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TvDirectionalDisc(
    size: Dp = 185.dp,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF121B2C))
            .border(2.dp, NeonCyan.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // UP
        DiscDirButton(
            label = "▲",
            key = TvRemoteKey.UP,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            onKey = onKey
        )

        // DOWN
        DiscDirButton(
            label = "▼",
            key = TvRemoteKey.DOWN,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            onKey = onKey
        )

        // LEFT
        DiscDirButton(
            label = "◀",
            key = TvRemoteKey.LEFT,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp),
            onKey = onKey
        )

        // RIGHT
        DiscDirButton(
            label = "▶",
            key = TvRemoteKey.RIGHT,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            onKey = onKey
        )

        // CENTER OK BUTTON
        var isOkPressed by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isOkPressed) NeonCyan else Color(0xFF1F304E)
                )
                .border(2.dp, if (isOkPressed) Color.White else NeonCyan, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isOkPressed = true
                            onKey(TvRemoteKey.OK, true)
                            tryAwaitRelease()
                            isOkPressed = false
                            onKey(TvRemoteKey.OK, false)
                        }
                    )
                }
                .testTag("remote_btn_ok"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "OK",
                color = if (isOkPressed) Color.Black else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun DiscDirButton(
    label: String,
    key: TvRemoteKey,
    modifier: Modifier = Modifier,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (isPressed) NeonCyan.copy(alpha = 0.45f) else Color(0xFF1B283F))
            .border(1.dp, if (isPressed) Color.White else Color(0xFF334668), CircleShape)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onKey(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        onKey(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else NeonCyan,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RemotePillHalfButton(
    icon: ImageVector,
    label: String,
    key: TvRemoteKey,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(if (isPressed) NeonCyan.copy(alpha = 0.4f) else Color.Transparent)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onKey(key, true)
                        tryAwaitRelease()
                        isPressed = false
                        onKey(key, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPressed) Color.White else Color(0xFFE2E8F0),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun RemoteMiniIconButton(
    icon: ImageVector,
    key: TvRemoteKey,
    accent: Color = Color.White,
    onClickCustom: (() -> Unit)? = null,
    onKey: (TvRemoteKey, Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isPressed) accent.copy(alpha = 0.35f) else Color(0xFF19253A))
            .border(1.dp, if (isPressed) Color.White else Color(0xFF2C3E5E), CircleShape)
            .pointerInput(key) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        if (onClickCustom != null) {
                            onClickCustom()
                        } else {
                            onKey(key, true)
                        }
                        tryAwaitRelease()
                        isPressed = false
                        if (onClickCustom == null) {
                            onKey(key, false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isPressed) Color.White else accent,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TvTouchpadView(
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onMoveDelta: (Float, Float) -> Unit
) {
    var isTouching by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131D30))
            .border(2.dp, if (isTouching) NeonPurple else Color(0xFF283B5E), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isTouching = true },
                    onDragEnd = { isTouching = false },
                    onDragCancel = { isTouching = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onMoveDelta(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.TouchApp, contentDescription = null, tint = if (isTouching) NeonPurple else NeonCyan, modifier = Modifier.size(34.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "بکشید برای حرکت ماوس • ضربه برای انتخاب (OK)",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class NumpadItem(val key: TvRemoteKey?, val text: String)

@Composable
private fun TvNumpadOverlay(
    onClose: () -> Unit,
    onNumberKey: (TvRemoteKey) -> Unit
) {
    val rows = remember {
        listOf(
            listOf(NumpadItem(TvRemoteKey.NUM_1, "1"), NumpadItem(TvRemoteKey.NUM_2, "2"), NumpadItem(TvRemoteKey.NUM_3, "3")),
            listOf(NumpadItem(TvRemoteKey.NUM_4, "4"), NumpadItem(TvRemoteKey.NUM_5, "5"), NumpadItem(TvRemoteKey.NUM_6, "6")),
            listOf(NumpadItem(TvRemoteKey.NUM_7, "7"), NumpadItem(TvRemoteKey.NUM_8, "8"), NumpadItem(TvRemoteKey.NUM_9, "9")),
            listOf(NumpadItem(null, ""), NumpadItem(TvRemoteKey.NUM_0, "0"), NumpadItem(null, ""))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .pointerInput(Unit) { detectTapGestures(onTap = { onClose() }) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F1728))
                .border(2.dp, NeonCyan, RoundedCornerShape(16.dp))
                .padding(16.dp)
                .pointerInput(Unit) { /* consume tap */ }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "صفحه‌کلید شماره‌ای تلویزیون",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        for (item in row) {
                            if (item.key != null) {
                                Button(
                                    onClick = { onNumberKey(item.key) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2D4A)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(item.text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            } else {
                                Spacer(modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("بستن", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
