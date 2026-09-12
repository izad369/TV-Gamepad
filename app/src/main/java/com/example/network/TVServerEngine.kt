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

class TVServerEngine(private val context: Context, private val scope: CoroutineScope) {
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
        serverName = name; pinCode = pin; stopServer(); _serverStatus.value = ConnectionStatus.CONNECTING; _serverIp.value = NetworkUtils.getLocalIpAddress()
        tcpJob = scope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket(ProtocolSerializer.DEFAULT_PORT); tcpServerSocket = server; _serverStatus.value = ConnectionStatus.CONNECTED
                while (isActive && !server.isClosed) try { val socket=server.accept(); socket.tcpNoDelay=true; handleClientConnection(socket) } catch (_: Exception) { if (!isActive) break }
            } catch (_: Exception) { _serverStatus.value = ConnectionStatus.ERROR }
        }
        udpJob = scope.launch(Dispatchers.IO) {
            try {
                val udp=DatagramSocket(ProtocolSerializer.DISCOVERY_PORT); udpDiscoverySocket=udp; val buffer=ByteArray(512)
                while (isActive && !udp.isClosed) { val p=DatagramPacket(buffer,buffer.size); udp.receive(p); if (String(p.data,0,p.length).trim()==ProtocolSerializer.DISCOVERY_MAGIC) { val reply="${ProtocolSerializer.DISCOVERY_ACK_PREFIX}$serverName:${ProtocolSerializer.DEFAULT_PORT}:$pinCode:${_players.value.size}"; val d=reply.toByteArray(); udp.send(DatagramPacket(d,d.size,p.address,p.port)) } }
            } catch (_: Exception) {}
        }
        btJob = scope.launch(Dispatchers.IO) { startBluetoothServer() }
    }

    private fun startBluetoothServer() {
        try {
            val adapter=BluetoothAdapter.getDefaultAdapter() ?: return; if (!adapter.isEnabled) return
            val server=adapter.listenUsingRfcommWithServiceRecord("TVGamepadServer",UUID.fromString(ProtocolSerializer.BT_UUID_STRING)); btServerSocket=server
            while (scope.isActive) try { handleBluetoothConnection(server.accept()) } catch (_: Exception) { break }
        } catch (_: SecurityException) {} catch (_: Exception) {}
    }

    private fun handleClientConnection(socket: Socket) = scope.launch(Dispatchers.IO) {
        val id=playerCounter.getAndIncrement(); addPlayer(ControllerPlayer(id,"Player $id",ConnectionType.WIFI,socket.inetAddress.hostAddress ?: "Unknown"))
        try { val reader=BufferedReader(InputStreamReader(socket.getInputStream())); val writer=PrintWriter(socket.getOutputStream(),true); while (isActive && !socket.isClosed) { val line=reader.readLine() ?: break; processInputLine(id,line,writer) } } catch (_: Exception) {} finally { removePlayer(id); runCatching { socket.close() } }
    }

    private fun handleBluetoothConnection(socket: BluetoothSocket) = scope.launch(Dispatchers.IO) {
        val id=playerCounter.getAndIncrement(); val name=try { socket.remoteDevice.name ?: "BT Controller" } catch (_: SecurityException) { "BT Controller" }; addPlayer(ControllerPlayer(id,"$name (P$id)",ConnectionType.BLUETOOTH,"Bluetooth"))
        try { val reader=BufferedReader(InputStreamReader(socket.inputStream)); val writer=PrintWriter(socket.outputStream,true); while (isActive && socket.isConnected) { val line=reader.readLine() ?: break; processInputLine(id,line,writer) } } catch (_: Exception) {} finally { removePlayer(id); runCatching { socket.close() } }
    }

    private fun processInputLine(id:Int,line:String,writer:PrintWriter?) {
        val p=line.trim().split(":"); if (p.isEmpty()) return
        when(p[0]) {
            "PING" -> if(p.size>1) writer?.println("PONG:${p[1].toLongOrNull() ?: 0L}")
            "B" -> if(p.size>=3) runCatching { GameButton.valueOf(p[1]) }.getOrNull()?.let { updatePlayerButton(id,it,p[2]=="1") }
            "S" -> if(p.size>=3) updatePlayerStick(id,p[1].toFloatOrNull() ?: 0f,p[2].toFloatOrNull() ?: 0f)
            "RS" -> if(p.size>=3) updatePlayerRightStick(id,p[1].toFloatOrNull() ?: 0f,p[2].toFloatOrNull() ?: 0f)
            "TR" -> if(p.size>=3) updatePlayerTriggers(id,p[1].toFloatOrNull() ?: 0f,p[2].toFloatOrNull() ?: 0f)
            "T" -> if(p.size>=3) updatePlayerTilt(id,p[1].toFloatOrNull() ?: 0f,p[2].toFloatOrNull() ?: 0f)
            "RK" -> if(p.size>=3) runCatching { com.example.model.TvRemoteKey.valueOf(p[1]) }.getOrNull()?.let { key -> val pressed=p[2]=="1"; _lastRemoteKey.value=Pair(key,pressed); when(key) { com.example.model.TvRemoteKey.UP->updatePlayerButton(id,GameButton.UP,pressed); com.example.model.TvRemoteKey.DOWN->updatePlayerButton(id,GameButton.DOWN,pressed); com.example.model.TvRemoteKey.LEFT->updatePlayerButton(id,GameButton.LEFT,pressed); com.example.model.TvRemoteKey.RIGHT->updatePlayerButton(id,GameButton.RIGHT,pressed); com.example.model.TvRemoteKey.OK->updatePlayerButton(id,GameButton.A,pressed); com.example.model.TvRemoteKey.BACK->updatePlayerButton(id,GameButton.B,pressed); com.example.model.TvRemoteKey.MENU->updatePlayerButton(id,GameButton.MENU,pressed); else->Unit } }
        }
    }

    private fun addPlayer(p:ControllerPlayer)=scope.launch(Dispatchers.Default){playersMutex.withLock{_players.value=_players.value.toMutableList().apply{add(p)}}}
    private fun removePlayer(id:Int)=scope.launch(Dispatchers.Default){playersMutex.withLock{_players.value=_players.value.filterNot{it.id==id}}}
    private fun updatePlayer(id:Int,fn:(ControllerPlayer)->ControllerPlayer)=scope.launch(Dispatchers.Default){playersMutex.withLock{val list=_players.value.toMutableList(); val i=list.indexOfFirst{it.id==id}; if(i>=0){list[i]=fn(list[i]);_players.value=list}}}
    private fun updatePlayerButton(id:Int,b:GameButton,pressed:Boolean)=updatePlayer(id){p->val s=p.inputState.pressedButtons.toMutableSet();if(pressed)s.add(b)else s.remove(b);p.copy(inputState=p.inputState.copy(pressedButtons=s,timestamp=System.currentTimeMillis()))}
    private fun updatePlayerStick(id:Int,x:Float,y:Float)=updatePlayer(id){it.copy(inputState=it.inputState.copy(stickX=x,stickY=y,timestamp=System.currentTimeMillis()))}
    private fun updatePlayerRightStick(id:Int,x:Float,y:Float)=updatePlayer(id){it.copy(inputState=it.inputState.copy(rightStickX=x,rightStickY=y,timestamp=System.currentTimeMillis()))}
    private fun updatePlayerTriggers(id:Int,l2:Float,r2:Float)=updatePlayer(id){it.copy(inputState=it.inputState.copy(l2Value=l2,r2Value=r2,timestamp=System.currentTimeMillis()))}
    private fun updatePlayerTilt(id:Int,x:Float,y:Float)=updatePlayer(id){it.copy(inputState=it.inputState.copy(tiltX=x,tiltY=y,timestamp=System.currentTimeMillis()))}

    fun stopServer(){tcpJob?.cancel();udpJob?.cancel();btJob?.cancel();runCatching{tcpServerSocket?.close()};runCatching{udpDiscoverySocket?.close()};runCatching{btServerSocket?.close()};tcpServerSocket=null;udpDiscoverySocket=null;btServerSocket=null;_players.value=emptyList();_serverStatus.value=ConnectionStatus.DISCONNECTED}
}
