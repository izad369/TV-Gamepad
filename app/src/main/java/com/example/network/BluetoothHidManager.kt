package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.GameButton
import com.example.model.TvRemoteKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

class BluetoothHidManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "BluetoothHidManager"

        // IMPORTANT: this version deliberately exposes ONE HID application only:
        // a gamepad. There are no keyboard/media collections and no Report IDs.
        // This avoids buggy TV HID hosts interpreting a gamepad report as
        // keyboard shortcuts or consumer-control commands.
        //
        // Report layout (9 bytes, because no Report ID is declared):
        //   byte 0-1 : 16 gamepad buttons
        //   byte 2   : D-pad hat switch (0..7, 8 = neutral/null)
        //   byte 3   : left stick X (-127..127)
        //   byte 4   : left stick Y (-127..127)
        //   byte 5   : right stick X (-127..127)
        //   byte 6   : right stick Y (-127..127)
        //   byte 7   : left trigger (0..255)
        //   byte 8   : right trigger (0..255)
        //
        // Button usages are explicit instead of a contiguous Usage Minimum /
        // Usage Maximum range, so Android TV sees the standard usages for
        // A/B/X/Y/L1/R1/L3/R3 in their actual bit positions.
        val HID_REPORT_DESCRIPTOR = byteArrayOf(
            // Game Pad application collection.
            0x05, 0x01,                         // Usage Page: Generic Desktop
            0x09, 0x05,                         // Usage: Game Pad
            0xA1.toByte(), 0x01,                // Collection: Application

            // 16 digital buttons.
            // Bits 0..12 are used by the app; bits 13..15 are reserved.
            0x05, 0x09,                         // Usage Page: Button
            0x09, 0x01,                         // A
            0x09, 0x02,                         // B
            0x09, 0x04,                         // X
            0x09, 0x05,                         // Y
            0x09, 0x03,                         // Z/custom action
            0x09, 0x07,                         // L1
            0x09, 0x08,                         // R1
            0x09, 0x09,                         // SELECT
            0x09, 0x0A,                         // START
            0x09, 0x0B,                         // HOME/custom action
            0x09, 0x0C,                         // MENU/custom action
            0x09, 0x0E,                         // L3
            0x09, 0x0F,                         // R3
            0x09, 0x10,                         // reserved
            0x09, 0x11,                         // reserved
            0x09, 0x12,                         // reserved
            0x15, 0x00,                         // Logical Minimum 0
            0x25, 0x01,                         // Logical Maximum 1
            0x75, 0x01,                         // Report Size 1
            0x95.toByte(), 0x10,                // Report Count 16
            0x81.toByte(), 0x02,                // Input: Data,Var,Abs

            // D-pad hat switch.
            // Android TV expects Generic Desktop Hat Switch with a 4-bit
            // field and a centered/null state.
            0x05, 0x01,
            0x09, 0x39,                         // Hat Switch
            0x15, 0x00,                         // Logical Minimum 0
            0x25, 0x07,                         // Logical Maximum 7
            0x35, 0x00,                         // Physical Minimum 0
            0x46, 0x3B, 0x01,                   // Physical Maximum 315
            0x65, 0x14,                         // Unit: degrees
            0x75, 0x04,                         // Report Size 4
            0x95.toByte(), 0x01,                // Report Count 1
            0x81.toByte(), 0x42,                // Data,Var,Abs + Null State
            0x75, 0x04,
            0x95.toByte(), 0x01,
            0x81.toByte(), 0x03,                // 4 bits padding

            // Left X/Y and right Z/Rz axes.
            0x05, 0x01,
            0x09, 0x30,                         // X
            0x09, 0x31,                         // Y
            0x09, 0x32,                         // Z
            0x09, 0x35,                         // Rz
            0x15, 0x81.toByte(),                // Logical Minimum -127
            0x25, 0x7F,                         // Logical Maximum 127
            0x75, 0x08,                         // Report Size 8
            0x95.toByte(), 0x04,                // Report Count 4
            0x81.toByte(), 0x02,                // Data,Var,Abs

            // L2/R2 analog triggers.
            0x05, 0x02,                         // Simulation Controls
            0x09.toByte(), 0xC5.toByte(),       // L2 / Brake
            0x09.toByte(), 0xC4.toByte(),       // R2 / Accelerator
            0x15, 0x00,
            0x26, 0xFF.toByte(), 0x00,           // 0..255
            0x75, 0x08,
            0x95.toByte(), 0x02,
            0x81.toByte(), 0x02,

            0xC0.toByte()                       // End Game Pad collection
        )
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var hidDevice: BluetoothHidDevice? = null
    private val executor = Executors.newSingleThreadExecutor()

    private val _isHidSupported = MutableStateFlow(false)
    val isHidSupported: StateFlow<Boolean> = _isHidSupported.asStateFlow()
    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()
    private val _statusMessage = MutableStateFlow("Initializing Bluetooth HID...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        @SuppressLint("MissingPermission")
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as? BluetoothHidDevice
                _isHidSupported.value = hidDevice != null
                if (hidDevice != null) {
                    _statusMessage.value = "HID Device Profile Connected"
                    registerHidApp()
                } else {
                    _statusMessage.value = "HID Device Profile Not Supported on this hardware"
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isRegistered.value = false
                _connectedDevice.value = null
                _statusMessage.value = "HID Service Disconnected"
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            _isRegistered.value = registered
            _statusMessage.value = if (registered) {
                "HID Ready! Ready to pair with Android TV"
            } else {
                "HID Registration Failed"
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedDevice.value = device
                    _statusMessage.value = "Connected to TV: ${device?.name ?: "Unknown"}"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (_connectedDevice.value == device) _connectedDevice.value = null
                    _statusMessage.value = "Disconnected from TV"
                }
                BluetoothProfile.STATE_CONNECTING -> _statusMessage.value = "Connecting to TV..."
            }
        }
    }

    init { initProfile() }

    private fun initProfile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val success = bluetoothAdapter?.getProfileProxy(
                    context,
                    profileListener,
                    BluetoothProfile.HID_DEVICE
                ) == true
                _isHidSupported.value = success
                if (!success) _statusMessage.value = "Bluetooth HID not available on this phone"
            } catch (e: Exception) {
                Log.e(TAG, "Error obtaining HID profile proxy", e)
                _statusMessage.value = "HID Error: ${e.message}"
            }
        } else {
            _isHidSupported.value = false
            _statusMessage.value = "Android 9 (Pie) or newer required for HID Gamepad"
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        val hid = hidDevice ?: return
        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "TV Gamepad HID",
                "Wireless Gamepad for Android TV",
                "Android",
                BluetoothHidDevice.SUBCLASS2_GAMEPAD,
                HID_REPORT_DESCRIPTOR
            )
            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800,
                9,
                0,
                11250,
                BluetoothHidDeviceAppQosSettings.MAX
            )
            hid.registerApp(sdp, null, qos, executor, hidCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID App", e)
            _statusMessage.value = "Registration error: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) { hidDevice?.connect(device) }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(device: BluetoothDevice) { hidDevice?.disconnect(device) }

    @SuppressLint("MissingPermission")
    fun sendGamepadReport(
        pressedButtons: Set<GameButton>,
        stickX: Float,
        stickY: Float,
        rightStickX: Float = 0f,
        rightStickY: Float = 0f,
        l2Value: Float = 0f,
        r2Value: Float = 0f
    ) {
        val target = _connectedDevice.value ?: return
        val hid = hidDevice ?: return

        // Bit positions match the explicit button usages in the descriptor.
        var buttonMask = 0
        if (pressedButtons.contains(GameButton.A)) buttonMask = buttonMask or (1 shl 0)
        if (pressedButtons.contains(GameButton.B)) buttonMask = buttonMask or (1 shl 1)
        if (pressedButtons.contains(GameButton.X)) buttonMask = buttonMask or (1 shl 2)
        if (pressedButtons.contains(GameButton.Y)) buttonMask = buttonMask or (1 shl 3)
        if (pressedButtons.contains(GameButton.Z)) buttonMask = buttonMask or (1 shl 4)
        if (pressedButtons.contains(GameButton.L1)) buttonMask = buttonMask or (1 shl 5)
        if (pressedButtons.contains(GameButton.R1)) buttonMask = buttonMask or (1 shl 6)
        if (pressedButtons.contains(GameButton.SELECT)) buttonMask = buttonMask or (1 shl 7)
        if (pressedButtons.contains(GameButton.START)) buttonMask = buttonMask or (1 shl 8)
        if (pressedButtons.contains(GameButton.HOME)) buttonMask = buttonMask or (1 shl 9)
        if (pressedButtons.contains(GameButton.MENU)) buttonMask = buttonMask or (1 shl 10)
        if (pressedButtons.contains(GameButton.L3)) buttonMask = buttonMask or (1 shl 11)
        if (pressedButtons.contains(GameButton.R3)) buttonMask = buttonMask or (1 shl 12)

        val up = pressedButtons.contains(GameButton.UP) || pressedButtons.contains(GameButton.D2_UP)
        val down = pressedButtons.contains(GameButton.DOWN) || pressedButtons.contains(GameButton.D2_DOWN)
        val left = pressedButtons.contains(GameButton.LEFT) || pressedButtons.contains(GameButton.D2_LEFT)
        val right = pressedButtons.contains(GameButton.RIGHT) || pressedButtons.contains(GameButton.D2_RIGHT)

        val hat: Byte = when {
            up && right -> 1
            down && right -> 3
            down && left -> 5
            up && left -> 7
            up -> 0
            right -> 2
            down -> 4
            left -> 6
            else -> 8
        }.toByte()

        val axisX = (stickX.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisY = (stickY.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisZ = (rightStickX.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisRz = (rightStickY.coerceIn(-1f, 1f) * 127f).toInt().toByte()

        val effectiveL2 = if (pressedButtons.contains(GameButton.L2)) 1f else l2Value.coerceIn(0f, 1f)
        val effectiveR2 = if (pressedButtons.contains(GameButton.R2)) 1f else r2Value.coerceIn(0f, 1f)
        val triggerL2 = (effectiveL2 * 255f).toInt().toByte()
        val triggerR2 = (effectiveR2 * 255f).toInt().toByte()

        val reportData = byteArrayOf(
            (buttonMask and 0xFF).toByte(),
            ((buttonMask shr 8) and 0xFF).toByte(),
            hat,
            axisX,
            axisY,
            axisZ,
            axisRz,
            triggerL2,
            triggerR2
        )

        // No Report IDs are declared, so Android's BluetoothHidDevice API
        // requires report id 0.
        hid.sendReport(target, 0, reportData)
    }

    // Remote/consumer commands are intentionally disabled in this diagnostic
    // gamepad-only build. Reintroducing them before the gamepad is verified
    // would reintroduce the mixed-HID parsing problem we are testing for.
    @SuppressLint("MissingPermission")
    fun sendRemoteKey(key: TvRemoteKey, pressed: Boolean) {
        Log.d(TAG, "Remote key ignored in gamepad-only HID build: $key / $pressed")
    }

    @SuppressLint("MissingPermission")
    fun release() {
        try {
            hidDevice?.unregisterApp()
            if (hidDevice != null) {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing HID", e)
        }
    }
}
