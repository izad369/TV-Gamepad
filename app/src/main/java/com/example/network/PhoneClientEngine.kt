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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class PhoneClientEngine(private val context: Context, private val scope: CoroutineScope) : SensorEventListener {
    private var tcpSocket: Socket? = null
    private var tcpWriter: PrintWriter? = null
    private var btSocket: BluetoothSocket? = null
    private var btWriter: PrintWriter? = null
    private var pingJob: Job? = null
    private var discoveryJob: Job? = null
    private val writeMutex = Mutex()

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

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isMotionControlEnabled = false
    val hidManager = BluetoothHidManager(context, scope)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun setMotionControlsEnabled(enabled: Boolean) {
        isMotionControlEnabled = enabled
        if (enabled) accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        else { sensorManager?.unregisterListener(this); sendTilt(0f, 0f) }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isMotionControlEnabled || event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        sendTilt((event.values[1] / 7f).coerceIn(-1f, 1f), (-event.values[0] / 7f).coerceIn(-1f, 1f))
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun triggerHaptic(durationMs: Long = 25) {
        try { vibrator?.let { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)) else @Suppress("DEPRECATION") it.vibrate(durationMs) } } catch (_: Exception) {}
    }

    fun startDiscovery() {
        stopDiscovery(); _discoveredHosts.value = emptyList()
        discoveryJob = scope.launch(Dispatchers.IO) {
            val lock = NetworkUtils.acquireMulticastLock(context)
            try {
                DatagramSocket().use { udp ->
                    udp.broadcast = true; udp.soTimeout = 2000
                    val data = ProtocolSerializer.DISCOVERY_MAGIC.toByteArray()
                    val targets = listOf(NetworkUtils.getBroadcastAddress(), java.net.InetAddress.getByName("255.255.255.255"))
                    while (isActive) {
                        targets.forEach { runCatching { udp.send(DatagramPacket(data, data.size, it, ProtocolSerializer.DISCOVERY_PORT)) } }
                        val packet = DatagramPacket(ByteArray(512), 512)
                        runCatching { udp.receive(packet) }.onSuccess {
                            val reply = String(packet.data, 0, packet.length).trim()
                            if (reply.startsWith(ProtocolSerializer.DISCOVERY_ACK_PREFIX)) {
                                val p = reply.removePrefix(ProtocolSerializer.DISCOVERY_ACK_PREFIX).split(":")
                                val hostIp = packet.address.hostAddress ?: "127.0.0.1"
                                val host = DiscoveredHost(p.getOrNull(0) ?: "TV Console", hostIp, p.getOrNull(1)?.toIntOrNull() ?: ProtocolSerializer.DEFAULT_PORT, p.getOrNull(2) ?: "", p.getOrNull(3)?.toIntOrNull() ?: 0)
                                _discoveredHosts.value = _discoveredHosts.value.toMutableList().apply { val i = indexOfFirst { it.ipAddress == hostIp && it.port == host.port }; if (i >= 0) this[i] = host else add(host) }
                            }
                        }
                        delay(1200)
                    }
                }
            } finally { runCatching { lock?.release() } }
        }
    }
    fun stopDiscovery() { discoveryJob?.cancel(); discoveryJob = null }

    fun connectWifi(ip: String, port: Int = ProtocolSerializer.DEFAULT_PORT, hostName: String = "TV") {
        disconnect(); _connectionType.value = ConnectionType.WIFI; _connectionStatus.value = ConnectionStatus.CONNECTING; _connectedHostName.value = hostName
        scope.launch(Dispatchers.IO) {
            try {
                val socket = Socket(); socket.tcpNoDelay = true; socket.connect(InetSocketAddress(ip, port), 4000); tcpSocket = socket; tcpWriter = PrintWriter(socket.getOutputStream(), true)
                _connectionStatus.value = ConnectionStatus.CONNECTED; startPingLoop()
                BufferedReader(InputStreamReader(socket.getInputStream())).use { reader -> while (isActive && socket.isConnected && !socket.isClosed) { val line = reader.readLine() ?: break; processIncomingLine(line) } }
            } catch (_: Exception) { if (_connectionStatus.value != ConnectionStatus.DISCONNECTED) _connectionStatus.value = ConnectionStatus.ERROR } finally { disconnect() }
        }
    }

    fun connectBluetooth(device: BluetoothDevice) {
        disconnect(); _connectionType.value = ConnectionType.BLUETOOTH; _connectionStatus.value = ConnectionStatus.CONNECTING; _connectedHostName.value = try { device.name ?: "BT TV" } catch (_: SecurityException) { "BT TV" }
        scope.launch(Dispatchers.IO) {
            try {
                val socket = device.createRfcommSocketToServiceRecord(UUID.fromString(ProtocolSerializer.BT_UUID_STRING)); btSocket = socket; socket.connect(); btWriter = PrintWriter(socket.outputStream, true)
                _connectionStatus.value = ConnectionStatus.CONNECTED; startPingLoop()
                BufferedReader(InputStreamReader(socket.inputStream)).use { reader -> while (isActive && socket.isConnected) { val line = reader.readLine() ?: break; processIncomingLine(line) } }
            } catch (_: Exception) { if (_connectionStatus.value != ConnectionStatus.DISCONNECTED) _connectionStatus.value = ConnectionStatus.ERROR } finally { disconnect() }
        }
    }

    fun getPairedBluetoothDevices(): List<BluetoothDevice> = try { BluetoothAdapter.getDefaultAdapter()?.takeIf { it.isEnabled }?.bondedDevices?.toList() ?: emptyList() } catch (_: SecurityException) { emptyList() }

    private fun startPingLoop() { pingJob?.cancel(); pingJob = scope.launch(Dispatchers.IO) { while (isActive && _connectionStatus.value == ConnectionStatus.CONNECTED) { sendRaw(ProtocolSerializer.encodePing(System.currentTimeMillis())); delay(1500) } } }
    private fun processIncomingLine(line: String) { if (line.trim().startsWith("PONG:")) line.trim().removePrefix("PONG:").toLongOrNull()?.let { _pingMs.value = (System.currentTimeMillis() - it).coerceAtLeast(1) } }

    fun onButtonEvent(button: GameButton, pressed: Boolean) {
        if (pressed) triggerHaptic(20)
        val buttons = _currentState.value.pressedButtons.toMutableSet().apply { if (pressed) add(button) else remove(button) }
        _currentState.value = _currentState.value.copy(pressedButtons = buttons, timestamp = System.currentTimeMillis())
        hidManager.sendGamepadReport(buttons, _currentState.value.stickX, _currentState.value.stickY, _currentState.value.rightStickX, _currentState.value.rightStickY, _currentState.value.l2Value, _currentState.value.r2Value)
        sendRaw(ProtocolSerializer.encodeButton(button, pressed))
    }
    fun onStickMove(x: Float, y: Float) { _currentState.value = _currentState.value.copy(stickX=x, stickY=y, timestamp=System.currentTimeMillis()); hidManager.sendGamepadReport(_currentState.value.pressedButtons,x,y,_currentState.value.rightStickX,_currentState.value.rightStickY,_currentState.value.l2Value,_currentState.value.r2Value); sendRaw(ProtocolSerializer.encodeStick(x,y)) }
    fun onRightStickMove(x: Float, y: Float) { _currentState.value = _currentState.value.copy(rightStickX=x,rightStickY=y,timestamp=System.currentTimeMillis()); hidManager.sendGamepadReport(_currentState.value.pressedButtons,_currentState.value.stickX,_currentState.value.stickY,x,y,_currentState.value.l2Value,_currentState.value.r2Value); sendRaw(ProtocolSerializer.encodeRightStick(x,y)) }
    fun onTriggersMove(l2: Float, r2: Float) { _currentState.value = _currentState.value.copy(l2Value=l2,r2Value=r2,timestamp=System.currentTimeMillis()); hidManager.sendGamepadReport(_currentState.value.pressedButtons,_currentState.value.stickX,_currentState.value.stickY,_currentState.value.rightStickX,_currentState.value.rightStickY,l2,r2); sendRaw(ProtocolSerializer.encodeTriggers(l2,r2)) }
    fun onRemoteKeyEvent(key: com.example.model.TvRemoteKey, pressed: Boolean) { if (pressed) triggerHaptic(25); hidManager.sendRemoteKey(key,pressed); sendRaw(ProtocolSerializer.encodeRemoteKey(key,pressed)) }
    private fun sendTilt(tx: Float, ty: Float) { _currentState.value = _currentState.value.copy(tiltX=tx,tiltY=ty,timestamp=System.currentTimeMillis()); sendRaw(ProtocolSerializer.encodeTilt(tx,ty)) }

    private fun sendRaw(msg: String) {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return
        scope.launch(Dispatchers.IO) { writeMutex.withLock { try { val line=msg.trimEnd('\r','\n'); tcpWriter?.println(line); btWriter?.println(line); tcpWriter?.flush(); btWriter?.flush() } catch (_: Exception) {} } }
    }

    fun disconnect() { pingJob?.cancel(); pingJob=null; runCatching { tcpSocket?.close() }; runCatching { btSocket?.close() }; tcpSocket=null; tcpWriter=null; btSocket=null; btWriter=null; _connectionStatus.value=ConnectionStatus.DISCONNECTED; _pingMs.value=0 }
}
