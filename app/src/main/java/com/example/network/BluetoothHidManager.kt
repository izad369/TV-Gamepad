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

        // GPlus/Android TV compatibility: keep the Consumer Control report in the
        // common 8-bit form used by standard media remotes. In particular, keep
        // Volume Decrement (0xEA) in bit 1 and do not put AC Back/Home in this
        // same bitfield; some TV firmware incorrectly aliases those usages.
        val HID_REPORT_DESCRIPTOR = byteArrayOf(
            // --- GAMEPAD (Report ID 1) ---
            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x05.toByte(),
            0xa1.toByte(), 0x01.toByte(),
            0x85.toByte(), 0x01.toByte(),
            0x05.toByte(), 0x09.toByte(),
            0x19.toByte(), 0x01.toByte(),
            0x29.toByte(), 0x10.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x10.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x39.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x07.toByte(),
            0x35.toByte(), 0x00.toByte(),
            0x46.toByte(), 0x3B.toByte(), 0x01.toByte(),
            0x65.toByte(), 0x14.toByte(),
            0x75.toByte(), 0x04.toByte(),
            0x95.toByte(), 0x01.toByte(),
            0x81.toByte(), 0x42.toByte(),
            0x75.toByte(), 0x04.toByte(),
            0x95.toByte(), 0x01.toByte(),
            0x81.toByte(), 0x03.toByte(),
            0x05.toByte(), 0x01.toByte(),
            0x09.toByte(), 0x30.toByte(),
            0x09.toByte(), 0x31.toByte(),
            0x09.toByte(), 0x32.toByte(),
            0x09.toByte(), 0x35.toByte(),
            0x15.toByte(), 0x81.toByte(),
            0x25.toByte(), 0x7F.toByte(),
            0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x04.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0x09.toByte(), 0x36.toByte(),
            0x09.toByte(), 0x37.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(),
            0x75.toByte(), 0x08.toByte(),
            0x95.toByte(), 0x02.toByte(),
            0x81.toByte(), 0x02.toByte(),
            0xc0.toByte(),

            // --- STANDARD 8-BIT CONSUMER CONTROL (Report ID 2) ---
            0x05.toByte(), 0x0c.toByte(),
            0x09.toByte(), 0x01.toByte(),
            0xa1.toByte(), 0x01.toByte(),
            0x85.toByte(), 0x02.toByte(),
            0x15.toByte(), 0x00.toByte(),
            0x25.toByte(), 0x01.toByte(),
            0x75.toByte(), 0x01.toByte(),
            0x95.toByte(), 0x08.toByte(),
            0x09.toByte(), 0xe9.toByte(), // bit 0: Volume Up
            0x09.toByte(), 0xea.toByte(), // bit 1: Volume Down
            0x09.toByte(), 0xe2.toByte(), // bit 2: Mute
            0x09.toByte(), 0xcd.toByte(), // bit 3: Play/Pause
            0x09.toByte(), 0x30.toByte(), // bit 4: Power
            0x09.toByte(), 0x9c.toByte(), // bit 5: Channel Up
            0x09.toByte(), 0x9d.toByte(), // bit 6: Channel Down
            0x09.toByte(), 0xb5.toByte(), // bit 7: Scan Next / Fast Forward
            0x81.toByte(), 0x02.toByte(),
            0xc0.toByte()
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
                } else _statusMessage.value = "HID Device Profile Not Supported on this hardware"
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
                BluetoothProfile.STATE_CONNECTED -> { _connectedDevice.value = device; _statusMessage.value = "Connected to TV: ${device?.name ?: "Unknown"}" }
                BluetoothProfile.STATE_DISCONNECTED -> { if (_connectedDevice.value == device) _connectedDevice.value = null; _statusMessage.value = "Disconnected from TV" }
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
            val sdp = BluetoothHidDeviceAppSdpSettings("TV Gamepad & Remote", "Wireless Gamepad & Remote for Android TV", "Android", BluetoothHidDevice.SUBCLASS1_COMBO, HID_REPORT_DESCRIPTOR)
            val qos = BluetoothHidDeviceAppQosSettings(BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT, 800, 9, 0, 11250, BluetoothHidDeviceAppQosSettings.MAX)
            hid.registerApp(sdp, null, qos, executor, hidCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register HID App", e)
            _statusMessage.value = "Registration error: ${e.localizedMessage}"
        }
    }

    @SuppressLint("MissingPermission") fun connectDevice(device: BluetoothDevice) { hidDevice?.connect(device) }
    @SuppressLint("MissingPermission") fun disconnectDevice(device: BluetoothDevice) { hidDevice?.disconnect(device) }

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
        if (pressedButtons.contains(GameButton.L2) || l2Value > 0.5f) buttonMask = buttonMask or (1 shl 7)
        if (pressedButtons.contains(GameButton.R2) || r2Value > 0.5f) buttonMask = buttonMask or (1 shl 8)
        if (pressedButtons.contains(GameButton.SELECT)) buttonMask = buttonMask or (1 shl 9)
        if (pressedButtons.contains(GameButton.START)) buttonMask = buttonMask or (1 shl 10)
        if (pressedButtons.contains(GameButton.MENU)) buttonMask = buttonMask or (1 shl 11)
        if (pressedButtons.contains(GameButton.HOME)) buttonMask = buttonMask or (1 shl 12)
        if (pressedButtons.contains(GameButton.L3)) buttonMask = buttonMask or (1 shl 13)
        if (pressedButtons.contains(GameButton.R3)) buttonMask = buttonMask or (1 shl 14)
        val up = pressedButtons.contains(GameButton.UP) || pressedButtons.contains(GameButton.D2_UP)
        val down = pressedButtons.contains(GameButton.DOWN) || pressedButtons.contains(GameButton.D2_DOWN)
        val left = pressedButtons.contains(GameButton.LEFT) || pressedButtons.contains(GameButton.D2_LEFT)
        val right = pressedButtons.contains(GameButton.RIGHT) || pressedButtons.contains(GameButton.D2_RIGHT)
        val hat: Byte = when {
            up && right -> 1; down && right -> 3; down && left -> 5; up && left -> 7
            up -> 0; right -> 2; down -> 4; left -> 6; else -> 8
        }.toByte()
        val axisX = (stickX.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisY = (stickY.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisZ = (rightStickX.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val axisRz = (rightStickY.coerceIn(-1f, 1f) * 127f).toInt().toByte()
        val triggerL2 = (l2Value.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val triggerR2 = (r2Value.coerceIn(0f, 1f) * 255f).toInt().toByte()
        val reportData = byteArrayOf((buttonMask and 0xFF).toByte(), ((buttonMask shr 8) and 0xFF).toByte(), hat, axisX, axisY, axisZ, axisRz, triggerL2, triggerR2)
        hid.sendReport(target, 1, reportData)
    }

    /**
     * Sends the GPlus-compatible 8-bit Consumer Control report.
     * Navigation keys are deliberately handled only by the gamepad report below;
     * AC Back/Home in the old 16-bit consumer bitfield could be aliased by the TV.
     */
    @SuppressLint("MissingPermission")
    fun sendRemoteKey(key: TvRemoteKey, pressed: Boolean) {
        val target = _connectedDevice.value ?: return
        val hid = hidDevice ?: return
        var consumerKeyMask = 0
        if (pressed) {
            when (key) {
                TvRemoteKey.VOL_UP -> consumerKeyMask = consumerKeyMask or (1 shl 0)
                TvRemoteKey.VOL_DOWN -> consumerKeyMask = consumerKeyMask or (1 shl 1)
                TvRemoteKey.MUTE -> consumerKeyMask = consumerKeyMask or (1 shl 2)
                TvRemoteKey.PLAY_PAUSE -> consumerKeyMask = consumerKeyMask or (1 shl 3)
                TvRemoteKey.POWER -> consumerKeyMask = consumerKeyMask or (1 shl 4)
                TvRemoteKey.CH_UP -> consumerKeyMask = consumerKeyMask or (1 shl 5)
                TvRemoteKey.CH_DOWN -> consumerKeyMask = consumerKeyMask or (1 shl 6)
                TvRemoteKey.FAST_FORWARD -> consumerKeyMask = consumerKeyMask or (1 shl 7)
                else -> Unit
            }
        }
        hid.sendReport(target, 2, byteArrayOf(consumerKeyMask.toByte()))

        when (key) {
            TvRemoteKey.UP, TvRemoteKey.DOWN, TvRemoteKey.LEFT, TvRemoteKey.RIGHT -> {
                val hat = if (pressed) when (key) {
                    TvRemoteKey.UP -> 0; TvRemoteKey.RIGHT -> 2; TvRemoteKey.DOWN -> 4; TvRemoteKey.LEFT -> 6; else -> 8
                } else 8
                hid.sendReport(target, 1, byteArrayOf(0, 0, hat.toByte(), 0, 0, 0, 0, 0, 0))
            }
            TvRemoteKey.OK -> {
                val mask = if (pressed) 1 else 0
                hid.sendReport(target, 1, byteArrayOf(mask.toByte(), 0, 8, 0, 0, 0, 0, 0, 0))
            }
            TvRemoteKey.BACK -> {
                val mask = if (pressed) (1 shl 1) else 0
                hid.sendReport(target, 1, byteArrayOf(mask.toByte(), 0, 8, 0, 0, 0, 0, 0, 0))
            }
            TvRemoteKey.HOME -> {
                val mask = if (pressed) (1 shl 12) else 0
                hid.sendReport(target, 1, byteArrayOf((mask and 0xFF).toByte(), ((mask shr 8) and 0xFF).toByte(), 8, 0, 0, 0, 0, 0, 0))
            }
            TvRemoteKey.MENU -> {
                val mask = if (pressed) (1 shl 11) else 0
                hid.sendReport(target, 1, byteArrayOf((mask and 0xFF).toByte(), ((mask shr 8) and 0xFF).toByte(), 8, 0, 0, 0, 0, 0, 0))
            }
            else -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    fun release() {
        try {
            hidDevice?.unregisterApp()
            if (hidDevice != null) bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        } catch (e: Exception) { Log.e(TAG, "Error releasing HID", e) }
    }
}
