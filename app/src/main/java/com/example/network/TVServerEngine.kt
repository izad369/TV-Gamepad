package com.example.network

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.model.ConnectionStatus
import com.example.model.ConnectionType
import com.example.model.ControllerPlayer
import com.example.model.GameButton
import com.example.model.ProtocolSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val playersMutex = Mutex()
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
                    } catch (_: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                _serverStatus.value = ConnectionStatus.ERROR
            }
        }

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
                        udpSocket.send(DatagramPacket(replyData, replyData.size, packet.address, packet.port))
                    }
                }
            } catch (_: Exception) {
                // Socket closes normally when server stops.
            }
        }

        btJob = scope.launch(Dispatchers.IO) { startBluetoothServer() }
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
                    handleBluetoothConnection(server.accept())
                } catch (_: Exception) {
                    break
                }
            }
        } catch (_: SecurityException) {
            // Bluetooth permission unavailable.
        } catch (_: Exception) {
            // Bluetooth server unavailable on this device.
        }
    }

    private fun handleClientConnection(socket: Socket) {
        scope.launch(Dispatchers.IO) {
            val playerId = playerCounter.getAndIncrement()
            addPlayer(ControllerPlayer(playerId, "Player $playerId", ConnectionType.WIFI, socket.inetAddress.hostAddress ?: "Unknown"))
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)
                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    processInputLine(playerId, line, writer)
                }
            } catch (_: Exception) {
                // Client disconnected.
            } finally {
                removePlayer(playerId)
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun handleBluetoothConnection(socket: BluetoothSocket) {
        scope.launch(Dispatchers.IO) {
            val playerId = playerCounter.getAndIncrement()
            val deviceName = try { socket.remoteDevice.name ?: "BT Controller" } catch (_: SecurityException) { "BT Controller" }
            addPlayer(ControllerPlayer(playerId, "$deviceName (P$playerId)", ConnectionType.BLUETOOTH, "Bluetooth"))
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                val writer = PrintWriter(socket.outputStream, true)
                while (isActive && !socket.isClosed) {
                    val line = reader.readLine() ?: break
                    processInputLine(playerId, line, writer)
                }
            } catch (_: Exception) {
                // Bluetooth client disconnected.
            } finally {
                removePlayer(playerId)
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun processInputLine(playerId: Int, line: String, writer: PrintWriter?) {
        val parts = line.trim().split(":")
        if (parts.isEmpty() || parts[0].isEmpty()) return
        when (parts[0]) {
            "PING" -> if (parts.size > 1) writer?.println("PONG:${parts[1].toLongOrNull() ?: 0L}")
            "B" -> if (parts.size >= 3) {
                val button = try { GameButton.valueOf(parts[1]) } catch (_: Exception) { null }
                if (button != null) updatePlayerButton(playerId, button, parts[2] == "1")
            }
            "S" -> if (parts.size >= 3) updatePlayerStick(playerId, parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f)
            "RS" -> if (parts.size >= 3) updatePlayerRightStick(playerId, parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f)
            "TR" -> if (parts.size >= 3) updatePlayerTriggers(playerId, parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f)
            "T" -> if (parts.size >= 3) updatePlayerTilt(playerId, parts[1].toFloatOrNull() ?: 0f, parts[2].toFloatOrNull() ?: 0f)
            "RK" -> if (parts.size >= 3) {
                val key = try { com.example.model.TvRemoteKey.valueOf(parts[1]) } catch (_: Exception) { null }
                if (key != null) {
                    val pressed = parts[2] == "1"
                    _lastRemoteKey.value = Pair(key, pressed)
                    when (key) {
                        com.example.model.TvRemoteKey.UP -> updatePlayerButton(playerId, GameButton.UP, pressed)
                        com.example.model.TvRemoteKey.DOWN -> updatePlayerButton(playerId, GameButton.DOWN, pressed)
                        com.example.model.TvRemoteKey.LEFT -> updatePlayerButton(playerId, GameButton.LEFT, pressed)
                        com.example.model.TvRemoteKey.RIGHT -> updatePlayerButton(playerId, GameButton.RIGHT, pressed)
                        com.example.model.TvRemoteKey.OK -> updatePlayerButton(playerId, GameButton.A, pressed)
                        com.example.model.TvRemoteKey.BACK -> updatePlayerButton(playerId, GameButton.B, pressed)
                        com.example.model.TvRemoteKey.MENU -> updatePlayerButton(playerId, GameButton.MENU, pressed)
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun addPlayer(player: ControllerPlayer) {
        scope.launch(Dispatchers.Default) {
            playersMutex.withLock {
                _players.value = _players.value.toMutableList().apply { add(player) }
            }
        }
    }

    private fun removePlayer(playerId: Int) {
        scope.launch(Dispatchers.Default) {
            playersMutex.withLock {
                _players.value = _players.value.filterNot { it.id == playerId }
            }
        }
    }

    private fun updatePlayer(playerId: Int, transform: (ControllerPlayer) -> ControllerPlayer) {
        scope.launch(Dispatchers.Default) {
            playersMutex.withLock {
                val current = _players.value.toMutableList()
                val index = current.indexOfFirst { it.id == playerId }
                if (index >= 0) {
                    current[index] = transform(current[index])
                    _players.value = current
                }
            }
        }
    }

    private fun updatePlayerButton(playerId: Int, button: GameButton, pressed: Boolean) = updatePlayer(playerId) { player ->
        val buttons = player.inputState.pressedButtons.toMutableSet()
        if (pressed) buttons.add(button) else buttons.remove(button)
        player.copy(inputState = player.inputState.copy(pressedButtons = buttons, timestamp = System.currentTimeMillis()))
    }

    private fun updatePlayerStick(playerId: Int, x: Float, y: Float) = updatePlayer(playerId) {
        it.copy(inputState = it.inputState.copy(stickX = x, stickY = y, timestamp = System.currentTimeMillis()))
    }

    private fun updatePlayerRightStick(playerId: Int, x: Float, y: Float) = updatePlayer(playerId) {
        it.copy(inputState = it.inputState.copy(rightStickX = x, rightStickY = y, timestamp = System.currentTimeMillis()))
    }

    private fun updatePlayerTriggers(playerId: Int, l2: Float, r2: Float) = updatePlayer(playerId) {
        it.copy(inputState = it.inputState.copy(l2Value = l2, r2Value = r2, timestamp = System.currentTimeMillis()))
    }

    private fun updatePlayerTilt(playerId: Int, tx: Float, ty: Float) = updatePlayer(playerId) {
        it.copy(inputState = it.inputState.copy(tiltX = tx, tiltY = ty, timestamp = System.currentTimeMillis()))
    }

    fun stopServer() {
        tcpJob?.cancel(); udpJob?.cancel(); btJob?.cancel()
        try { tcpServerSocket?.close() } catch (_: Exception) {}
        try { udpDiscoverySocket?.close() } catch (_: Exception) {}
        try { btServerSocket?.close() } catch (_: Exception) {}
        tcpServerSocket = null; udpDiscoverySocket = null; btServerSocket = null
        _players.value = emptyList()
        _serverStatus.value = ConnectionStatus.DISCONNECTED
    }
}
