package com.example.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.example.model.ConnectionStatus
import com.example.model.ConnectionType
import com.example.model.ControllerPlayer
import com.example.model.GameButton
import com.example.model.GamepadInputState
import com.example.model.ProtocolSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class TVServerEngine(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var tcpServerSocket: ServerSocket? = null
    private var udpDiscoverySocket: DatagramSocket? = null
    private var btServerSocket: BluetoothServerSocket? = null

    private var tcpJob: Job? = null
    private var udpJob: Job? = null
    private var btJob: Job? = null

    private val playerCounter = AtomicInteger(1)

    private val _players = MutableStateFlow<List<ControllerPlayer>>(emptyList())
    val players: StateFlow<List<ControllerPlayer>> = _players.asStateFlow()

    private val _lastRemoteKey = MutableStateFlow<Pair<com.example.model.TvRemoteKey, Boolean>?>(null)
    val lastRemoteKey: StateFlow<Pair<com.example.model.TvRemoteKey, Boolean>?> = _lastRemoteKey.asStateFlow()

    private val _serverStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val serverStatus: StateFlow<ConnectionStatus> = _serverStatus.asStateFlow()

    private val _serverIp = MutableStateFlow("127.0.0.1")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private val _serverPort = MutableStateFlow(ProtocolSerializer.DEFAULT_PORT)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    var serverName: String = "Android TV Console"
    var pinCode: String = "1234"

    fun startServer(name: String = "Android TV Console", pin: String = "1234") {
        serverName = name
        pinCode = pin
        stopServer()

        _serverStatus.value = ConnectionStatus.CONNECTING
        _serverIp.value = NetworkUtils.getLocalIpAddress()

        // 1. Start TCP Server
        tcpJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(ProtocolSerializer.DEFAULT_PORT)
                tcpServerSocket = server
                _serverStatus.value = ConnectionStatus.CONNECTED

                while (isActive && !server.isClosed) {
                    try {
                        val clientSocket = server.accept()
                        clientSocket.tcpNoDelay = true
                        handleClientConnection(clientSocket)
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _serverStatus.value = ConnectionStatus.ERROR
            }
        }

        // 2. Start UDP Discovery Responder
        udpJob = scope.launch(Dispatchers.IO) {
            try {
                val udpSocket = DatagramSocket(ProtocolSerializer.DISCOVERY_PORT)
                udpDiscoverySocket = udpSocket
                val buffer = ByteArray(512)

                while (isActive && !udpSocket.isClosed) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    udpSocket.receive(packet)
                    val message = String(packet.data, 0, packet.length).trim()

                    if (message == ProtocolSerializer.DISCOVERY_MAGIC) {
                        val reply = "${ProtocolSerializer.DISCOVERY_ACK_PREFIX}$serverName:${ProtocolSerializer.DEFAULT_PORT}:$pinCode:${_players.value.size}"
                        val replyData = reply.toByteArray()
                        val replyPacket = DatagramPacket(replyData, replyData.size, packet.address, packet.port)
                        udpSocket.send(replyPacket)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. Start Bluetooth RFCOMM Server if available
        btJob = scope.launch(Dispatchers.IO) {
            startBluetoothServer()
        }
    }

    private fun startBluetoothServer() {
        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter() ?: return
            if (!btAdapter.isEnabled) return

            val uuid = UUID.fromString(ProtocolSerializer.BT_UUID_STRING)
            val server = btAdapter.listenUsingRfcommWithServiceRecord("TVGamepadServer", uuid)
            btServerSocket = server

            while (scope.isActive) {
                try {
                    val socket = server.accept()
                    handleBluetoothConnection(socket)
                } catch (e: Exception) {
                    break
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted or older device
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClientConnection(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            val playerId = playerCounter.getAndIncrement()
            val clientAddress = socket.inetAddress.hostAddress ?: "Unknown"
            var player = ControllerPlayer(
                id = playerId,
                name = "Player $playerId",
                connectionType = ConnectionType.WIFI,
                ipOrAddress = clientAddress
            )
            addPlayer(player)

            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                while (isActive && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    processInputLine(playerId, line, writer)
                }
            } catch (e: Exception) {
                // Client disconnected
            } finally {
                removePlayer(playerId)
                try { socket.close() } catch (ignored: Exception) {}
            }
        }
    }

    private fun handleBluetoothConnection(socket: BluetoothSocket) {
        scope.launch(Dispatchers.IO) {
            val playerId = playerCounter.getAndIncrement()
            val deviceName = try { socket.remoteDevice.name ?: "BT Controller" } catch (e: SecurityException) { "BT Controller" }
            val player = ControllerPlayer(
                id = playerId,
                name = "$deviceName (P$playerId)",
                connectionType = ConnectionType.BLUETOOTH,
                ipOrAddress = "Bluetooth"
            )
            addPlayer(player)

            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                val writer = PrintWriter(socket.outputStream, true)

                while (isActive && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    processInputLine(playerId, line, writer)
                }
            } catch (e: Exception) {
                // BT Disconnected
            } finally {
                removePlayer(playerId)
                try { socket.close() } catch (ignored: Exception) {}
            }
        }
    }

    private fun processInputLine(playerId: Int, line: String, writer: PrintWriter?) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return

        val parts = trimmed.split(":")
        if (parts.isEmpty()) return

        when (parts[0]) {
            "PING" -> {
                if (parts.size > 1) {
                    writer?.println(ProtocolSerializer.encodePong(parts[1].toLongOrNull() ?: 0L))
                }
            }
            "B" -> {
                // Button event: B:A:1 or B:A:0
                if (parts.size >= 3) {
                    val buttonName = parts[1]
                    val pressed = parts[2] == "1"
                    val button = try { GameButton.valueOf(buttonName) } catch (e: Exception) { null }
                    if (button != null) {
                        updatePlayerButton(playerId, button, pressed)
                    }
                }
            }
            "S" -> {
                // Stick event: S:0.25:-0.80
                if (parts.size >= 3) {
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f
                    updatePlayerStick(playerId, x, y)
                }
            }
            "TR" -> {
                // Triggers: TR:0.5:1.0
                if (parts.size >= 3) {
                    val l2 = parts[1].toFloatOrNull() ?: 0f
                    val r2 = parts[2].toFloatOrNull() ?: 0f
                    updatePlayerTriggers(playerId, l2, r2)
                }
            }
            "T" -> {
                // Tilt gyro: T:0.12:-0.05
                if (parts.size >= 3) {
                    val tx = parts[1].toFloatOrNull() ?: 0f
                    val ty = parts[2].toFloatOrNull() ?: 0f
                    updatePlayerTilt(playerId, tx, ty)
                }
            }
            "RK" -> {
                // Remote Key: RK:UP:1
                if (parts.size >= 3) {
                    val keyName = parts[1]
                    val pressed = parts[2] == "1"
                    val key = try { com.example.model.TvRemoteKey.valueOf(keyName) } catch (e: Exception) { null }
                    if (key != null) {
                        _lastRemoteKey.value = Pair(key, pressed)
                        when (key) {
                            com.example.model.TvRemoteKey.UP -> updatePlayerButton(playerId, GameButton.UP, pressed)
                            com.example.model.TvRemoteKey.DOWN -> updatePlayerButton(playerId, GameButton.DOWN, pressed)
                            com.example.model.TvRemoteKey.LEFT -> updatePlayerButton(playerId, GameButton.LEFT, pressed)
                            com.example.model.TvRemoteKey.RIGHT -> updatePlayerButton(playerId, GameButton.RIGHT, pressed)
                            com.example.model.TvRemoteKey.OK -> updatePlayerButton(playerId, GameButton.A, pressed)
                            com.example.model.TvRemoteKey.BACK -> updatePlayerButton(playerId, GameButton.B, pressed)
                            com.example.model.TvRemoteKey.MENU -> updatePlayerButton(playerId, GameButton.MENU, pressed)
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun addPlayer(player: ControllerPlayer) {
        val current = _players.value.toMutableList()
        current.add(player)
        _players.value = current
    }

    private fun removePlayer(playerId: Int) {
        val current = _players.value.toMutableList()
        current.removeAll { it.id == playerId }
        _players.value = current
    }

    private fun updatePlayerButton(playerId: Int, button: GameButton, pressed: Boolean) {
        val current = _players.value.toMutableList()
        val index = current.indexOfFirst { it.id == playerId }
        if (index != -1) {
            val player = current[index]
            val buttons = player.inputState.pressedButtons.toMutableSet()
            if (pressed) buttons.add(button) else buttons.remove(button)
            val updatedState = player.inputState.copy(
                pressedButtons = buttons,
                timestamp = System.currentTimeMillis()
            )
            current[index] = player.copy(inputState = updatedState)
            _players.value = current
        }
    }

    private fun updatePlayerStick(playerId: Int, x: Float, y: Float) {
        val current = _players.value.toMutableList()
        val index = current.indexOfFirst { it.id == playerId }
        if (index != -1) {
            val player = current[index]
            val updatedState = player.inputState.copy(
                stickX = x,
                stickY = y,
                timestamp = System.currentTimeMillis()
            )
            current[index] = player.copy(inputState = updatedState)
            _players.value = current
        }
    }

    private fun updatePlayerTriggers(playerId: Int, l2: Float, r2: Float) {
        val current = _players.value.toMutableList()
        val index = current.indexOfFirst { it.id == playerId }
        if (index != -1) {
            val player = current[index]
            val updatedState = player.inputState.copy(
                l2Value = l2,
                r2Value = r2,
                timestamp = System.currentTimeMillis()
            )
            current[index] = player.copy(inputState = updatedState)
            _players.value = current
        }
    }

    private fun updatePlayerTilt(playerId: Int, tx: Float, ty: Float) {
        val current = _players.value.toMutableList()
        val index = current.indexOfFirst { it.id == playerId }
        if (index != -1) {
            val player = current[index]
            val updatedState = player.inputState.copy(
                tiltX = tx,
                tiltY = ty,
                timestamp = System.currentTimeMillis()
            )
            current[index] = player.copy(inputState = updatedState)
            _players.value = current
        }
    }

    fun stopServer() {
        try { tcpJob?.cancel() } catch (ignored: Exception) {}
        try { udpJob?.cancel() } catch (ignored: Exception) {}
        try { btJob?.cancel() } catch (ignored: Exception) {}
        try { tcpServerSocket?.close() } catch (ignored: Exception) {}
        try { udpDiscoverySocket?.close() } catch (ignored: Exception) {}
        try { btServerSocket?.close() } catch (ignored: Exception) {}
        _players.value = emptyList()
        _serverStatus.value = ConnectionStatus.DISCONNECTED
    }
}
