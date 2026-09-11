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

/**
 * Manages standard Bluetooth HID (Human Interface Device) emulation.
 * Allows this Android phone to register as a genuine hardware Gamepad & TV Remote
 * over Bluetooth HID, enabling it to control ANY game and system menu on Android TV
 * without requiring custom apps or input mappers on the TV!
 */
class BluetoothHidManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "BluetoothHidManager"

        // Standard USB/Bluetooth HID Gamepad + Consumer Control (TV Remote) Report Descriptor
        val HID_REPORT_DESCRIPTOR = byteArrayOf(
            // --- GAMEPAD (Report ID 1) ---
            0x05.toByte(), 0x01.toByte(), // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x05.toByte(), // USAGE (Gamepad)
            0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x01.toByte(), //   REPORT_ID (1)

            // 16 Gamepad Buttons: A, B, X, Y, L1, R1, L2, R2, Select, Start, Menu, ThumbL, ThumbR, etc.
            0x05.toByte(), 0x09.toByte(), //   USAGE_PAGE (Button)
            0x19.toByte(), 0x01.toByte(), //   USAGE_MINIMUM (Button 1)
            0x29.toByte(), 0x10.toByte(), //   USAGE_MAXIMUM (Button 16)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
            0x95.toByte(), 0x10.toByte(), //   REPORT_COUNT (16)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)

            // Hat switch (D-Pad): 4 bits (0-7 direction, 8 = neutral)
            0x05.toByte(), 0x01.toByte(), //   USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x39.toByte(), //   USAGE (Hat switch)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x07.toByte(), //   LOGICAL_MAXIMUM (7)
            0x35.toByte(), 0x00.toByte(), //   PHYSICAL_MINIMUM (0)
            0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // PHYSICAL_MAXIMUM (315)
            0x65.toByte(), 0x14.toByte(), //   UNIT (Eng Rot:Angular Pos)
            0x75.toByte(), 0x04.toByte(), //   REPORT_SIZE (4)
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x81.toByte(), 0x42.toByte(), //   INPUT (Data,Var,Abs,Null)
            // Padding 4 bits
            0x75.toByte(), 0x04.toByte(), //   REPORT_SIZE (4)
            0x95.toByte(), 0x01.toByte(), //   REPORT_COUNT (1)
            0x81.toByte(), 0x03.toByte(), //   INPUT (Cnst,Var,Abs)

            // Analog Sticks: X, Y, Z, Rz (-127 to 127)
            0x05.toByte(), 0x01.toByte(), //   USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x30.toByte(), //   USAGE (X - Left Stick X)
            0x09.toByte(), 0x31.toByte(), //   USAGE (Y - Left Stick Y)
            0x09.toByte(), 0x32.toByte(), //   USAGE (Z - Right Stick X)
            0x09.toByte(), 0x35.toByte(), //   USAGE (Rz - Right Stick Y)
            0x15.toByte(), 0x81.toByte(), //   LOGICAL_MINIMUM (-127)
            0x25.toByte(), 0x7F.toByte(), //   LOGICAL_MAXIMUM (127)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x95.toByte(), 0x04.toByte(), //   REPORT_COUNT (4)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)

            // Analog Triggers: L2 Slider, R2 Dial (0 to 255)
            0x09.toByte(), 0x36.toByte(), //   USAGE (Slider - L2)
            0x09.toByte(), 0x37.toByte(), //   USAGE (Dial - R2)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
            0x75.toByte(), 0x08.toByte(), //   REPORT_SIZE (8)
            0x95.toByte(), 0x02.toByte(), //   REPORT_COUNT (2)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)
            0xc0.toByte(), // END_COLLECTION

            // --- CONSUMER CONTROL / TV REMOTE (Report ID 2) ---
            0x05.toByte(), 0x0c.toByte(), // USAGE_PAGE (Consumer Devices)
            0x09.toByte(), 0x01.toByte(), // USAGE (Consumer Control)
            0xa1.toByte(), 0x01.toByte(), // COLLECTION (Application)
            0x85.toByte(), 0x02.toByte(), //   REPORT_ID (2)
            0x15.toByte(), 0x00.toByte(), //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(), //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(), //   REPORT_SIZE (1)
            0x95.toByte(), 0x10.toByte(), //   REPORT_COUNT (16)
            0x09.toByte(), 0xe9.toByte(), //   USAGE (Volume Increment)
            0x09.toByte(), 0xea.toByte(), //   USAGE (Volume Decrement)
            0x09.toByte(), 0xe2.toByte(), //   USAGE (Mute)
            0x09.toByte(), 0xcd.toByte(), //   USAGE (Play/Pause)
            0x09.toByte(), 0x30.toByte(), //   USAGE (Power)
            0x09.toByte(), 0x24.toByte(), //   USAGE (Back)
            0x09.toByte(), 0x23.toByte(), //   USAGE (Home)
            0x09.toByte(), 0x40.toByte(), //   USAGE (Menu)
            0x09.toByte(), 0x41.toByte(), //   USAGE (Select / OK)
            0x09.toByte(), 0x42.toByte(), //   USAGE (Up)
            0x09.toByte(), 0x43.toByte(), //   USAGE (Down)
            0x09.toByte(), 0x44.toByte(), //   USAGE (Left)
            0x09.toByte(), 0x45.toByte(), //   USAGE (Right)
            0x09.toByte(), 0xb5.toByte(), //   USAGE (Scan Next Track)
            0x09.toByte(), 0xb6.toByte(), //   USAGE (Scan Previous Track)
            0x09.toByte(), 0xb7.toByte(), //   USAGE (Stop)
            0x81.toByte(), 0x02.toByte(), //   INPUT (Data,Var,Abs)
            0xc0.toByte()  // END_COLLECTION
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
            if (registered) {
                _statusMessage.value = "HID Ready! Ready to pair with Android TV"
            } else {
                _statusMessage.value = "HID Registration Failed"
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectedDevice.value = device
                    _statusMessage.value = "Connected to TV: ${device?.name ?: "Unknown"}"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (_connectedDevice.value == device) {
                        _connectedDevice.value = null
                    }
                    _statusMessage.value = "Disconnected from TV"
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _statusMessage.value = "Connecting to TV..."
                }
            }
        }
    }

    init {
        initProfile()
    }

    private fun initProfile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val success = bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE) == true
                _isHidSupported.value = success
                if (!success) {
                    _statusMessage.value = "Bluetooth HID not available on this phone"
                }
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
                "TV Gamepad & Remote",
                "Wireless Gamepad & Remote for Android TV",
                "Android",
                BluetoothHidDevice.SUBCLASS1_COMBO,
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
    fun connectDevice(device: BluetoothDevice) {
        hidDevice?.connect(device)
    }

    @SuppressLint("MissingPermission")
    fun disconnectDevice(device: BluetoothDevice) {
        hidDevice?.disconnect(device)
    }

    /**
     * Sends Gamepad input report (Report ID 1)
     */
    @SuppressLint("MissingPermission")
    fun sendGamepadReport(
        pressedButtons: Set<GameButton>,
        stickX: Float,
        stickY: Float,
        l2Value: Float = 0f,
        r2Value: Float = 0f
    ) {
        val target = _connectedDevice.value ?: return
        val hid = hidDevice ?: return

        // 16 bits for buttons
        var buttonMask = 0
        if (pressedButtons.contains(GameButton.A)) buttonMask = buttonMask or (1 shl 0)
        if (pressedButtons.contains(GameButton.B)) buttonMask = buttonMask or (1 shl 1)
        if (pressedButtons.contains(GameButton.X)) buttonMask = buttonMask or (1 shl 2)
        if (pressedButtons.contains(GameButton.Y)) buttonMask = buttonMask or (1 shl 3)
        if (pressedButtons.contains(GameButton.L1)) buttonMask = buttonMask or (1 shl 4)
        if (pressedButtons.contains(GameButton.R1)) buttonMask = buttonMask or (1 shl 5)
        if (pressedButtons.contains(GameButton.L2) || l2Value > 0.5f) buttonMask = buttonMask or (1 shl 6)
        if (pressedButtons.contains(GameButton.R2) || r2Value > 0.5f) buttonMask = buttonMask or (1 shl 7)
        if (pressedButtons.contains(GameButton.SELECT)) buttonMask = buttonMask or (1 shl 8)
        if (pressedButtons.contains(GameButton.START)) buttonMask = buttonMask or (1 shl 9)
        if (pressedButtons.contains(GameButton.MENU)) buttonMask = buttonMask or (1 shl 10)

        // D-Pad / Hat switch: 0=Up, 1=UpRight, 2=Right, 3=DownRight, 4=Down, 5=DownLeft, 6=Left, 7=UpLeft, 8=Neutral
        val up = pressedButtons.contains(GameButton.UP)
        val down = pressedButtons.contains(GameButton.DOWN)
        val left = pressedButtons.contains(GameButton.LEFT)
        val right = pressedButtons.contains(GameButton.RIGHT)

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

        // Thumbstick axes (-127 to 127)
        val axisX = (stickX.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisY = (stickY.coerceIn(-1f, 1f) * 127f).toInt().toByte()

        // Triggers (0 to 255)
        val triggerL2 = (l2Value.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val triggerR2 = (r2Value.coerceIn(0f, 1f) * 255f).toInt().toByte()

        val reportData = byteArrayOf(
            (buttonMask and 0xFF).toByte(),
            ((buttonMask shr 8) and 0xFF).toByte(),
            hat,
            axisX,
            axisY,
            0, // Right stick X
            0, // Right stick Y
            triggerL2,
            triggerR2
        )

        hid.sendReport(target, 1, reportData)
    }

    /**
     * Sends Consumer Control (TV Remote) report (Report ID 2)
     */
    @SuppressLint("MissingPermission")
    fun sendRemoteKey(key: TvRemoteKey, pressed: Boolean) {
        val target = _connectedDevice.value ?: return
        val hid = hidDevice ?: return

        var keyMask = 0
        if (pressed) {
            when (key) {
                TvRemoteKey.VOL_UP -> keyMask = keyMask or (1 shl 0)
                TvRemoteKey.VOL_DOWN -> keyMask = keyMask or (1 shl 1)
                TvRemoteKey.MUTE -> keyMask = keyMask or (1 shl 2)
                TvRemoteKey.PLAY_PAUSE -> keyMask = keyMask or (1 shl 3)
                TvRemoteKey.POWER -> keyMask = keyMask or (1 shl 4)
                TvRemoteKey.BACK -> keyMask = keyMask or (1 shl 5)
                TvRemoteKey.HOME -> keyMask = keyMask or (1 shl 6)
                TvRemoteKey.MENU -> keyMask = keyMask or (1 shl 7)
                TvRemoteKey.OK -> keyMask = keyMask or (1 shl 8)
                TvRemoteKey.UP -> keyMask = keyMask or (1 shl 9)
                TvRemoteKey.DOWN -> keyMask = keyMask or (1 shl 10)
                TvRemoteKey.LEFT -> keyMask = keyMask or (1 shl 11)
                TvRemoteKey.RIGHT -> keyMask = keyMask or (1 shl 12)
                TvRemoteKey.FAST_FORWARD -> keyMask = keyMask or (1 shl 13)
                TvRemoteKey.REWIND -> keyMask = keyMask or (1 shl 14)
            }
        }

        val reportData = byteArrayOf(
            (keyMask and 0xFF).toByte(),
            ((keyMask shr 8) and 0xFF).toByte()
        )

        hid.sendReport(target, 2, reportData)
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
