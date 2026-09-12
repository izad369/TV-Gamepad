package com.example.ui.gamepad

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DiscoveredHost
import com.example.network.PhoneClientEngine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow

@Composable
fun ConnectionDialog(
    clientEngine: PhoneClientEngine,
    onDismiss: () -> Unit,
    onConnectWifi: (ip: String, port: Int, name: String) -> Unit,
    onConnectBt: (device: BluetoothDevice) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var manualIp by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("8888") }

    val discoveredHosts by clientEngine.discoveredHosts.collectAsStateWithLifecycle()
    var pairedDevices by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    fun refreshBluetooth() {
        pairedDevices = clientEngine.getPairedBluetoothDevices()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            clientEngine.startDiscovery()
        } else {
            clientEngine.stopDiscovery()
        }
        if (selectedTab == 1) {
            refreshBluetooth()
        }
    }

    Dialog(
        onDismissRequest = {
            clientEngine.stopDiscovery()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0D1627))
                .border(2.dp, NeonCyan.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "لیست دستگاه‌ها و اتصال به تلویزیون",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    IconButton(
                        onClick = {
                            clientEngine.stopDiscovery()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2E4A))
                            .testTag("close_connection_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Bluetooth HID (Direct) vs Wifi Auto vs Manual IP
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF142036),
                    contentColor = NeonCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonCyan,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "۱. بلوتوث (اتصال مستقیم HID)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) NeonCyan else Color(0xFFE2E8F0)
                            )
                        },
                        icon = { Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (selectedTab == 0) NeonCyan else Color.White, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "۲. وای‌فای خودکار",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) NeonCyan else Color(0xFFE2E8F0)
                            )
                        },
                        icon = { Icon(Icons.Default.Wifi, contentDescription = null, tint = if (selectedTab == 1) NeonCyan else Color.White, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "۳. اتصال دستی (IP)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 2) NeonCyan else Color(0xFFE2E8F0)
                            )
                        },
                        icon = { Icon(Icons.Default.Lan, contentDescription = null, tint = if (selectedTab == 2) NeonCyan else Color.White, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content (Takes remaining space)
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (selectedTab) {
                        0 -> {
                            // =========================================================
                            // TAB 0: BLUETOOTH HID (Most Recommended)
                            // =========================================================
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Action & Info Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "بدون نیاز به نصب هیچ برنامه‌ای روی تلویزیون (پروفایل دسته فیزیکی):",
                                            color = NeonYellow,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { refreshBluetooth() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2F4D)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تازه‌سازی لیست", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Settings, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تنظیمات بلوتوث گوشی", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (pairedDevices.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF131D31))
                                            .border(1.dp, Color(0xFF26395B), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "هیچ دستگاه جفت‌شده‌ای در بلوتوث پیدا نشد!",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "۱. بلوتوث تلویزیون خود را روشن کنید.\n۲. دکمه «تنظیمات بلوتوث گوشی» بالا را بزنید و تلویزیون را جفت (Pair) کنید.\n۳. برگردید و دکمه «تازه‌سازی لیست» را لمس کنید.",
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 12.sp,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(pairedDevices) { device ->
                                            val devName = try { device.name ?: "تلویزیون / دستگاه هوشمند" } catch (e: SecurityException) { "تلویزیون" }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF152238))
                                                    .border(1.5.dp, Color(0xFF2B4168), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        clientEngine.hidManager.connectDevice(device)
                                                        onConnectBt(device)
                                                        onDismiss()
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(NeonCyan.copy(alpha = 0.2f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(Icons.Default.Tv, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(devName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                                        Text("آدرس: ${device.address} • اتصال به عنوان دسته بازی فیزیکی", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        clientEngine.hidManager.connectDevice(device)
                                                        onConnectBt(device)
                                                        onDismiss()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("اتصال مستقیم", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // =========================================================
                            // TAB 1: WIFI AUTO-DISCOVERY
                            // =========================================================
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = NeonCyan,
                                            strokeWidth = 2.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "در حال پویش خودکار تلویزیون در شبکه وای‌فای...",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Button(
                                        onClick = { clientEngine.startDiscovery() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2F4D)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("جستجوی مجدد", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (discoveredHosts.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF131D31))
                                            .border(1.dp, Color(0xFF26395B), RoundedCornerShape(12.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "هیچ سرور وای‌فایی روی شبکه یافت نشد.",
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "نکته مهم: برای استفاده بدون نصب برنامه، تب «بلوتوث (اتصال مستقیم HID)» را انتخاب کنید.\nدر غیر این صورت مطمئن شوید گوشی و تلویزیون به یک مودم وای‌فای متصل هستند.",
                                                color = Color(0xFFE2E8F0),
                                                fontSize = 12.sp,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(discoveredHosts) { host ->
                                            DiscoveredHostItemRow(host = host) {
                                                onConnectWifi(host.ipAddress, host.port, host.name)
                                                clientEngine.stopDiscovery()
                                                onDismiss()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            // =========================================================
                            // TAB 2: MANUAL IP
                            // =========================================================
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "آدرس IP نمایش‌داده شده روی تلویزیون را دستی وارد کنید:",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualIp,
                                        onValueChange = { manualIp = it },
                                        label = { Text("آدرس IP (مانند 192.168.1.100)", color = Color(0xFF94A3B8)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(2f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0xFF334668),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )

                                    OutlinedTextField(
                                        value = manualPort,
                                        onValueChange = { manualPort = it },
                                        label = { Text("پورت (پیش‌فرض 8888)", color = Color(0xFF94A3B8)) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color(0xFF334668),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (manualIp.isNotBlank()) {
                                            val port = manualPort.toIntOrNull() ?: 8888
                                            onConnectWifi(manualIp.trim(), port, "TV ($manualIp)")
                                            clientEngine.stopDiscovery()
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(0.5f).height(42.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("اتصال با IP", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveredHostItemRow(
    host: DiscoveredHost,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF152238))
            .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable { onConnect() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NeonGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Tv, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(host.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("${host.ipAddress}:${host.port} • بازیکنان: ${host.connectedPlayers}", color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }

        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("اتصال وای‌فای", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}
