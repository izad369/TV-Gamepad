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

        // Report 1 = standard Android-TV gamepad
        // Report 2 = consumer/media controls
        // Report 3 = keyboard-style TV navigation/numeric keys
        val HID_REPORT_DESCRIPTOR = byteArrayOf(
            // GAMEPAD, Report ID 1
            0x05, 0x01, 0x09, 0x05, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x01,

            // Button usages follow Android's documented TV game-controller mapping:
            // A=1, B=2, X=4, Y=5, L1=7, R1=8, L3=14, R3=15.
            // The remaining slots are custom/unused but stay inside the gamepad CA.
            0x05, 0x09,
            0x19, 0x01, 0x29, 0x02,
            0x19, 0x04, 0x29, 0x05,
            0x19, 0x03, 0x29, 0x03,
            0x19, 0x07, 0x29, 0x08,
            0x19, 0x09, 0x29, 0x0C,
            0x19, 0x0D, 0x29, 0x11,
            0x15, 0x00, 0x25, 0x01,
            0x75, 0x01, 0x95.toByte(), 0x10,
            0x81.toByte(), 0x02,

            // D-pad hat switch: logical 0..7, physical 0..315 degrees,
            // 4-bit report with a null state for the centered position.
            0x05, 0x01, 0x09, 0x39,
            0x15, 0x00, 0x25, 0x07,
            0x35, 0x00, 0x46, 0x3B, 0x01,
            0x65, 0x14,
            0x75, 0x04, 0x95.toByte(), 0x01,
            0x81.toByte(), 0x42,
            0x75, 0x04, 0x95.toByte(), 0x01,
            0x81.toByte(), 0x03,

            // Left stick X/Y and right stick Z/Rz.
            0x05, 0x01,
            0x09, 0x30, 0x09, 0x31, 0x09, 0x32, 0x09, 0x35,
            0x15, 0x81.toByte(), 0x25, 0x7F,
            0x75, 0x08, 0x95.toByte(), 0x04,
            0x81.toByte(), 0x02,

            // Left/Right triggers using Android's Simulation Controls usages.
            0x05, 0x02,
            0x09.toByte(), 0xC5.toByte(), 0x09.toByte(), 0xC4.toByte(),
            0x15, 0x00, 0x26, 0xFF.toByte(), 0x00,
            0x75, 0x08, 0x95.toByte(), 0x02,
            0x81.toByte(), 0x02,
            0xC0.toByte(),

            // CONSUMER CONTROL, Report ID 2
            0x05, 0x0C, 0x09, 0x01, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x02,
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08,
            0x09.toByte(), 0xE9.toByte(),
            0x09.toByte(), 0xEA.toByte(),
            0x09.toByte(), 0xE2.toByte(),
            0x09.toByte(), 0xCD.toByte(),
            0x09.toByte(), 0x30,
            0x09.toByte(), 0x9C.toByte(),
            0x09.toByte(), 0x9D.toByte(),
            0x09.toByte(), 0xB5.toByte(),
            0x81.toByte(), 0x02,
            0xC0.toByte(),

            // KEYBOARD, Report ID 3.
            0x05, 0x01, 0x09, 0x06, 0xA1.toByte(), 0x01, 0x85.toByte(), 0x03,
            0x05, 0x07, 0x19.toByte(), 0xE0.toByte(), 0x29, 0xE7.toByte(),
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08,
            0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x08, 0x81.toByte(), 0x01,
            0x95.toByte(), 0x06, 0x75, 0x08,
            0x15, 0x00, 0x25, 0x65,
            0x19, 0x00, 0x29, 0x65,
            0x81.toByte(), 0x00,
            0xC0.toByte()
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
            _statusMessage.value = if (registered) "HID Ready! Ready to pair with Android TV" else "HID Registration Failed"
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
                val success = bluetoothAdapter?.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE) == true
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
                "Wireless Gamepad and Remote for Android TV",
                "Android",
                // IMPORTANT: advertise as a GAMEPAD, not as a keyboard+mouse combo.
                // The previous COMBO subclass could make some TV HID hosts apply the
                // wrong input interpretation even though the report descriptor was correct.
                BluetoothHidDevice.SUBCLASS2_GAMEPAD,
                HID_REPORT_DESCRIPTOR
            )
            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT, 800, 9, 0, 11250,
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
        pressedButtons: Set<GameButton>, stickX: Float, stickY: Float,
        rightStickX: Float = 0f, rightStickY: Float = 0f,
        l2Value: Float = 0f, r2Value: Float = 0f
    ) {
        val target = _connectedDevice.value ?: return
        val hid = hidDevice ?: return

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
        if (pressedButtons.contains(GameButton.MENU)) buttonMask = buttonMask or (1 shl 9)
        if (pressedButtons.contains(GameButton.HOME)) buttonMask = buttonMask or (1 shl 10)
        if (pressedButtons.contains(GameButton.L3)) buttonMask = buttonMask or (1 shl 12)
        if (pressedButtons.contains(GameButton.R3)) buttonMask = buttonMask or (1 shl 13)

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
        hid.sendReport(target, 1, reportData)
    }

    @SuppressLint("MissingPermission")
    private fun sendConsumerKey(target: BluetoothDevice, hid: BluetoothHidDevice, key: TvRemoteKey, pressed: Boolean) {
        var mask = 0
        if (pressed) {
            mask = when (key) {
                TvRemoteKey.VOL_UP -> 1 shl 0
                TvRemoteKey.VOL_DOWN -> 1 shl 1
                TvRemoteKey.MUTE -> 1 shl 2
                TvRemoteKey.PLAY_PAUSE -> 1 shl 3
                TvRemoteKey.POWER -> 1 shl 4
                TvRemoteKey.CH_UP -> 1 shl 5
                TvRemoteKey.CH_DOWN -> 1 shl 6
                TvRemoteKey.FAST_FORWARD -> 1 shl 7
                else -> 0
            }
        }
        hid.sendReport(target, 2, byteArrayOf(mask.toByte()))
    }

    @SuppressLint("MissingPermission")
    private fun sendKeyboardUsage(target: BluetoothDevice, hid: BluetoothHidDevice, usage: Int, pressed: Boolean) {
        val report = ByteArray(8)
        if (pressed) report[2] = usage.toByte()
        hid.sendReport(target, 3, report)
    }

    @SuppressLint("MissingPermission")
    fun sendRemoteKey(key: TvRemoteKey, pressed: Boolean) {
        val target = _connectedDevice.value ?: return
        val hid = hidDevice ?: return

        when (key) {
            TvRemoteKey.VOL_UP, TvRemoteKey.VOL_DOWN, TvRemoteKey.MUTE,
            TvRemoteKey.PLAY_PAUSE, TvRemoteKey.POWER, TvRemoteKey.CH_UP,
            TvRemoteKey.CH_DOWN, TvRemoteKey.FAST_FORWARD -> sendConsumerKey(target, hid, key, pressed)

            TvRemoteKey.UP -> sendKeyboardUsage(target, hid, 0x52, pressed)
            TvRemoteKey.DOWN -> sendKeyboardUsage(target, hid, 0x51, pressed)
            TvRemoteKey.LEFT -> sendKeyboardUsage(target, hid, 0x50, pressed)
            TvRemoteKey.RIGHT -> sendKeyboardUsage(target, hid, 0x4F, pressed)
            TvRemoteKey.OK -> sendKeyboardUsage(target, hid, 0x28, pressed)
            TvRemoteKey.BACK -> sendKeyboardUsage(target, hid, 0x29, pressed)
            TvRemoteKey.HOME -> sendKeyboardUsage(target, hid, 0x4A, pressed)
            TvRemoteKey.MENU -> sendKeyboardUsage(target, hid, 0x65, pressed)
            TvRemoteKey.NUM_0 -> sendKeyboardUsage(target, hid, 0x27, pressed)
            TvRemoteKey.NUM_1 -> sendKeyboardUsage(target, hid, 0x1E, pressed)
            TvRemoteKey.NUM_2 -> sendKeyboardUsage(target, hid, 0x1F, pressed)
            TvRemoteKey.NUM_3 -> sendKeyboardUsage(target, hid, 0x20, pressed)
            TvRemoteKey.NUM_4 -> sendKeyboardUsage(target, hid, 0x21, pressed)
            TvRemoteKey.NUM_5 -> sendKeyboardUsage(target, hid, 0x22, pressed)
            TvRemoteKey.NUM_6 -> sendKeyboardUsage(target, hid, 0x23, pressed)
            TvRemoteKey.NUM_7 -> sendKeyboardUsage(target, hid, 0x24, pressed)
            TvRemoteKey.NUM_8 -> sendKeyboardUsage(target, hid, 0x25, pressed)
            TvRemoteKey.NUM_9 -> sendKeyboardUsage(target, hid, 0x26, pressed)
            TvRemoteKey.REWIND -> sendKeyboardUsage(target, hid, 0x4C, pressed)
            TvRemoteKey.INPUT_SOURCE -> sendKeyboardUsage(target, hid, 0x2B, pressed)
            TvRemoteKey.SETTINGS -> sendKeyboardUsage(target, hid, 0x65, pressed)
        }
    }

    @SuppressLint("MissingPermission")
    fun release() {
        try {
            hidDevice?.unregisterApp()
            if (hidDevice != null) bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing HID", e)
        }
    }
}
