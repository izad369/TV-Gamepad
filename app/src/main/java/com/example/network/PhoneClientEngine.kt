package com.example.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.ConnectionStatus
import com.example.model.ConnectionType
import com.example.model.DiscoveredHost
import com.example.model.GameButton
import com.example.model.GamepadInputState
import com.example.model.ProtocolSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class PhoneClientEngine(
    private val context: Context,
    private val scope: CoroutineScope
) : SensorEventListener {

    private var tcpSocket: Socket? = null
    private var tcpWriter: PrintWriter? = null
    private var btSocket: BluetoothSocket? = null
    private var btWriter: PrintWriter? = null

    private var readJob: Job? = null
    private var pingJob: Job? = null
    private var discoveryJob: Job? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionType = MutableStateFlow(ConnectionType.WIFI)
    val connectionType: StateFlow<ConnectionType> = _connectionType.asStateFlow()

    private val _connectedHostName = MutableStateFlow("")
    val connectedHostName: StateFlow<String> = _connectedHostName.asStateFlow()

    private val _pingMs = MutableStateFlow<Long>(0)
    val pingMs: StateFlow<Long> = _pingMs.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private val _currentState = MutableStateFlow(GamepadInputState())
    val currentState: StateFlow<GamepadInputState> = _currentState.asStateFlow()

    // Sensor Manager for Tilt / Motion Controls
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isMotionControlEnabled = false

    val hidManager = BluetoothHidManager(context, scope)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMotionControlsEnabled(enabled: Boolean) {
        isMotionControlEnabled = enabled
        if (enabled) {
            accelerometer?.let {
                sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            }
        } else {
            sensorManager?.unregisterListener(this)
            sendTilt(0f, 0f)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isMotionControlEnabled || event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // X and Y tilt normalized
            // When phone is in landscape, event.values[1] is tilt left/right, values[0] is up/down
            val rawX = (event.values[1] / 7.0f).coerceIn(-1f, 1f)
            val rawY = (-event.values[0] / 7.0f).coerceIn(-1f, 1f)
            sendTilt(rawX, rawY)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun triggerHaptic(durationMs: Long = 25) {
        try {
            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- DISCOVERY ---
    fun startDiscovery() {
        stopDiscovery()
        _discoveredHosts.value = emptyList()

        discoveryJob = scope.launch(Dispatchers.IO) {
            val lock = NetworkUtils.acquireMulticastLock(context)
            try {
                val udpSocket = DatagramSocket()
                udpSocket.broadcast = true
                udpSocket.soTimeout = 2000

                val broadcastAddr = NetworkUtils.getBroadcastAddress()
                val sendData = ProtocolSerializer.DISCOVERY_MAGIC.toByteArray()
                val sendPacket = DatagramPacket(
                    sendData,
                    sendData.size,
                    broadcastAddr,
                    ProtocolSerializer.DISCOVERY_PORT
                )

                // Also broadcast to 255.255.255.255
                val sendPacketGeneric = DatagramPacket(
                    sendData,
                    sendData.size,
                    java.net.InetAddress.getByName("255.255.255.255"),
                    ProtocolSerializer.DISCOVERY_PORT
                )

                while (isActive) {
                    try {
                        udpSocket.send(sendPacket)
                        udpSocket.send(sendPacketGeneric)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Receive responses
                    val buffer = ByteArray(512)
                    val recvPacket = DatagramPacket(buffer, buffer.size)
                    try {
                        udpSocket.receive(recvPacket)
                        val reply = String(recvPacket.data, 0, recvPacket.length).trim()
                        if (reply.startsWith(ProtocolSerializer.DISCOVERY_ACK_PREFIX)) {
                            val payload = reply.removePrefix(ProtocolSerializer.DISCOVERY_ACK_PREFIX)
                            val parts = payload.split(":")
                            val tvName = parts.getOrNull(0) ?: "TV Console"
                            val port = parts.getOrNull(1)?.toIntOrNull() ?: ProtocolSerializer.DEFAULT_PORT
                            val pin = parts.getOrNull(2) ?: ""
                            val playersCount = parts.getOrNull(3)?.toIntOrNull() ?: 0
                            val hostIp = recvPacket.address.hostAddress ?: "127.0.0.1"

                            val host = DiscoveredHost(
                                name = tvName,
                                ipAddress = hostIp,
                                port = port,
                                pin = pin,
                                connectedPlayers = playersCount
                            )

                            val list = _discoveredHosts.value.toMutableList()
                            val idx = list.indexOfFirst { it.ipAddress == hostIp && it.port == port }
                            if (idx >= 0) {
                                list[idx] = host
                            } else {
                                list.add(host)
                            }
                            _discoveredHosts.value = list
                        }
                    } catch (e: Exception) {
                        // socket timeout, loop again
                    }

                    delay(1200)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { lock?.release() } catch (ignored: Exception) {}
            }
        }
    }

    fun stopDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
    }

    // --- WI-FI CONNECT ---
    fun connectWifi(ip: String, port: Int = ProtocolSerializer.DEFAULT_PORT, hostName: String = "TV") {
        disconnect()
        _connectionType.value = ConnectionType.WIFI
        _connectionStatus.value = ConnectionStatus.CONNECTING
        _connectedHostName.value = hostName

        scope.launch(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(ip, port), 4000)
                tcpSocket = socket
                val writer = PrintWriter(socket.getOutputStream(), true)
                tcpWriter = writer

                _connectionStatus.value = ConnectionStatus.CONNECTED

                // Start ping loop
                startPingLoop { t -> writer.println(ProtocolSerializer.encodePing(t)) }

                // Start reader loop
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                while (isActive && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    processIncomingLine(line)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionStatus.value = ConnectionStatus.ERROR
            } finally {
                disconnect()
            }
        }
    }

    // --- BLUETOOTH CONNECT ---
    fun connectBluetooth(device: BluetoothDevice) {
        disconnect()
        _connectionType.value = ConnectionType.BLUETOOTH
        _connectionStatus.value = ConnectionStatus.CONNECTING
        val name = try { device.name ?: "BT TV" } catch (e: SecurityException) { "BT TV" }
        _connectedHostName.value = name

        scope.launch(Dispatchers.IO) {
            try {
                val uuid = UUID.fromString(ProtocolSerializer.BT_UUID_STRING)
                val socket = device.createRfcommSocketToServiceRecord(uuid)
                btSocket = socket
                socket.connect()
                val writer = PrintWriter(socket.outputStream, true)
                btWriter = writer

                _connectionStatus.value = ConnectionStatus.CONNECTED

                startPingLoop { t -> writer.println(ProtocolSerializer.encodePing(t)) }

                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                while (isActive && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    processIncomingLine(line)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionStatus.value = ConnectionStatus.ERROR
            } finally {
                disconnect()
            }
        }
    }

    fun getPairedBluetoothDevices(): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter?.isEnabled == true) {
                adapter.bondedDevices?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun startPingLoop(sendPing: (Long) -> Unit) {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) {
                try {
                    sendPing(System.currentTimeMillis())
                } catch (ignored: Exception) {}
                delay(1500)
            }
        }
    }

    private fun processIncomingLine(line: String) {
        val trimmed = line.trim()
        if (trimmed.startsWith("PONG:")) {
            val sentTime = trimmed.removePrefix("PONG:").toLongOrNull() ?: 0L
            if (sentTime > 0) {
                val latency = (System.currentTimeMillis() - sentTime).coerceAtLeast(1)
                _pingMs.value = latency
            }
        }
    }

    // --- INPUT TRANSMISSION ---
    fun onButtonEvent(button: GameButton, pressed: Boolean) {
        if (pressed) triggerHaptic(20)

        val currentButtons = _currentState.value.pressedButtons.toMutableSet()
        if (pressed) currentButtons.add(button) else currentButtons.remove(button)
        _currentState.value = _currentState.value.copy(
            pressedButtons = currentButtons,
            timestamp = System.currentTimeMillis()
        )

        hidManager.sendGamepadReport(
            currentButtons,
            _currentState.value.stickX,
            _currentState.value.stickY,
            _currentState.value.l2Value,
            _currentState.value.r2Value
        )
        sendRaw(ProtocolSerializer.encodeButton(button, pressed))
    }

    fun onStickMove(x: Float, y: Float) {
        _currentState.value = _currentState.value.copy(
            stickX = x,
            stickY = y,
            timestamp = System.currentTimeMillis()
        )
        hidManager.sendGamepadReport(
            _currentState.value.pressedButtons,
            x,
            y,
            _currentState.value.l2Value,
            _currentState.value.r2Value
        )
        sendRaw(ProtocolSerializer.encodeStick(x, y))
    }

    fun onTriggersMove(l2: Float, r2: Float) {
        _currentState.value = _currentState.value.copy(
            l2Value = l2,
            r2Value = r2,
            timestamp = System.currentTimeMillis()
        )
        hidManager.sendGamepadReport(
            _currentState.value.pressedButtons,
            _currentState.value.stickX,
            _currentState.value.stickY,
            l2,
            r2
        )
        sendRaw(ProtocolSerializer.encodeTriggers(l2, r2))
    }

    fun onRemoteKeyEvent(key: com.example.model.TvRemoteKey, pressed: Boolean) {
        if (pressed) triggerHaptic(25)
        hidManager.sendRemoteKey(key, pressed)
        sendRaw(ProtocolSerializer.encodeRemoteKey(key, pressed))
    }

    private fun sendTilt(tx: Float, ty: Float) {
        _currentState.value = _currentState.value.copy(
            tiltX = tx,
            tiltY = ty,
            timestamp = System.currentTimeMillis()
        )
        sendRaw(ProtocolSerializer.encodeTilt(tx, ty))
    }

    private fun sendRaw(msg: String) {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        scope.launch(Dispatchers.IO) {
            try {
                tcpWriter?.print(msg)
                tcpWriter?.flush()
                btWriter?.print(msg)
                btWriter?.flush()
            } catch (e: Exception) {
                // Ignore transient write error
            }
        }
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        try { tcpSocket?.close() } catch (ignored: Exception) {}
        try { btSocket?.close() } catch (ignored: Exception) {}
        tcpSocket = null
        tcpWriter = null
        btSocket = null
        btWriter = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _pingMs.value = 0
    }
}
