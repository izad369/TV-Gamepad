package com.example

import android.Manifest
import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.DeviceRole
import com.example.network.HttpFileServer
import com.example.network.PhoneClientEngine
import com.example.network.TVServerEngine
import com.example.ui.gamepad.GamepadScreen
import com.example.ui.theme.CyberCardBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.tv.TVConsoleScreen

class MainActivity : ComponentActivity() {

    private var tvServerEngine: TVServerEngine? = null
    private var phoneClientEngine: PhoneClientEngine? = null
    private var httpFileServer: HttpFileServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Auto-detect TV hardware and set landscape
        if (isRunningOnTv(this)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        setContent {
            MyApplicationTheme {
                MainAppRoot(
                    onEnginesReady = { server, client, fileServer ->
                        tvServerEngine = server
                        phoneClientEngine = client
                        httpFileServer = fileServer
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tvServerEngine?.stopServer()
        phoneClientEngine?.disconnect()
        httpFileServer?.stopServer()
    }
}

/**
 * Checks if the current Android environment is an Android TV / Google TV device.
 */
fun isRunningOnTv(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    val isTvUiMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    val pm = context.packageManager
    val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    val hasTvHardware = pm.hasSystemFeature("android.hardware.type.television")
    return isTvUiMode || hasLeanback || hasTvHardware
}

@Composable
fun MainAppRoot(
    onEnginesReady: (TVServerEngine, PhoneClientEngine, HttpFileServer) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isTvDevice = remember { isRunningOnTv(context) }

    val serverEngine = remember { TVServerEngine(context, coroutineScope) }
    val clientEngine = remember { PhoneClientEngine(context, coroutineScope) }
    val fileServer = remember { HttpFileServer(context, coroutineScope) }

    LaunchedEffect(Unit) {
        onEnginesReady(serverEngine, clientEngine, fileServer)
    }

    // Default directly to TV_CONSOLE if running on an Android TV device
    var currentRole by remember {
        mutableStateOf<DeviceRole?>(if (isTvDevice) DeviceRole.TV_CONSOLE else null)
    }

    // Handle Auto-Orientation:
    // If TV is detected, force horizontal (landscape) screen aspect ratio as requested:
    // "اگه برنامه بفهمه که ما در تلویزون هستیم نسبت صفحه خودکار به نسبت صفحه تلویزیون یعنی افقی تبدیل بشه"
    LaunchedEffect(currentRole, isTvDevice) {
        val activity = context as? Activity
        if (isTvDevice || currentRole == DeviceRole.TV_CONSOLE) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else if (currentRole == DeviceRole.PHONE_CONTROLLER) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Request Bluetooth permissions on Android 12+
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (permissionsToRequest.isNotEmpty()) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentRole) {
                DeviceRole.PHONE_CONTROLLER -> {
                    GamepadScreen(
                        clientEngine = clientEngine,
                        httpFileServer = fileServer,
                        onSwitchToTvMode = {
                            currentRole = DeviceRole.TV_CONSOLE
                        }
                    )
                }
                DeviceRole.TV_CONSOLE -> {
                    TVConsoleScreen(
                        serverEngine = serverEngine,
                        onSwitchToPhoneMode = {
                            serverEngine.stopServer()
                            currentRole = DeviceRole.PHONE_CONTROLLER
                        }
                    )
                }
                null -> {
                    RoleSelectionScreen(
                        isTvDevice = isTvDevice,
                        onSelectRole = { role -> currentRole = role }
                    )
                }
            }
        }
    }
}

@Composable
fun RoleSelectionScreen(
    isTvDevice: Boolean,
    onSelectRole: (DeviceRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF142036), CyberDarkBg),
                    radius = 1200f
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Logo Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(NeonCyan, Color(0xFF0D47A1))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = "Game Controller",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "گیم پد و کنترل تلویزیون",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isTvDevice) "تلویزیون شناسایی شد • جهت‌گیری افقی فعال است" else "دسته بازی بیسیم، کنترل و ارسال فایل به تلویزیون\nبدون نیاز به نصب برنامه روی تلویزیون",
                color = if (isTvDevice) NeonGreen else Color.LightGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role 1: Phone Gamepad & Remote & File transfer
            FocusableRoleCard(
                title = "📱 حالت گوشی (گیم‌پد، ریموت و ارسال فایل)",
                subtitle = "استفاده از گوشی به عنوان دسته بازی حرفه‌ای (XYZ)، کنترل تلویزیون (Home, Back) و ارسال فایل به مرورگر تلویزیون.",
                accentColor = NeonCyan,
                icon = Icons.Default.PhoneAndroid,
                tag = "select_phone_role",
                onClick = { onSelectRole(DeviceRole.PHONE_CONTROLLER) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Role 2: TV Console Receiver
            FocusableRoleCard(
                title = "📺 حالت تلویزیون (دریافت‌کننده کنسول)",
                subtitle = "در صورت نصب مستقیم روی تلویزیون هوشمند با پشتیبانی کامل از ریموت فیزیکی تلویزیون.",
                accentColor = NeonGreen,
                icon = Icons.Default.Tv,
                tag = "select_tv_role",
                onClick = { onSelectRole(DeviceRole.TV_CONSOLE) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "⚡ پشتیبانی مستقیم از بلوتوث HID بدون نصب روی TV + انتقال فایل در شبکه محلی",
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}


@Composable
private fun FocusableRoleCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isFocused) accentColor else accentColor.copy(alpha = 0.5f),
        label = "roleBorder"
    )

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF1B2B48) else CyberCardBg
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth(0.88f)
            .border(if (isFocused) 3.dp else 1.5.dp, animatedBorderColor, RoundedCornerShape(16.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = if (isFocused) Color.White else Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
