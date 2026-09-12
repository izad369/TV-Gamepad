package com.example.model

enum class GameButton(val label: String) {
    UP("▲"),
    DOWN("▼"),
    LEFT("◀"),
    RIGHT("▶"),
    A("A"),
    B("B"),
    X("X"),
    Y("Y"),
    Z("Z"),
    L1("L1"),
    R1("R1"),
    L2("L2"),
    R2("R2"),
    L3("L3"),
    R3("R3"),
    SELECT("SELECT"),
    START("START"),
    MENU("MENU"),
    HOME("HOME"),
    // Secondary Directional cluster
    D2_UP("▲2"),
    D2_DOWN("▼2"),
    D2_LEFT("◀2"),
    D2_RIGHT("▶2")
}

enum class TvRemoteKey(val label: String) {
    UP("▲"),
    DOWN("▼"),
    LEFT("◀"),
    RIGHT("▶"),
    OK("OK"),
    BACK("بازگشت"),
    HOME("خانه"),
    MENU("منو"),
    VOL_UP("صدا +"),
    VOL_DOWN("صدا -"),
    MUTE("بی‌صدا"),
    CH_UP("کانال +"),
    CH_DOWN("کانال -"),
    PLAY_PAUSE("پخش/توقف"),
    POWER("پاور"),
    REWIND("عقب"),
    FAST_FORWARD("جلو"),
    INPUT_SOURCE("ورودی"),
    SETTINGS("تنظیمات"),
    NUM_0("0"),
    NUM_1("1"),
    NUM_2("2"),
    NUM_3("3"),
    NUM_4("4"),
    NUM_5("5"),
    NUM_6("6"),
    NUM_7("7"),
    NUM_8("8"),
    NUM_9("9")
}

data class GamepadInputState(
    val pressedButtons: Set<GameButton> = emptySet(),
    val stickX: Float = 0f,
    val stickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val l2Value: Float = 0f,
    val r2Value: Float = 0f,
    val tiltX: Float = 0f,
    val tiltY: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

enum class DeviceRole {
    PHONE_CONTROLLER,
    TV_CONSOLE
}

enum class ConnectionType {
    WIFI,
    BLUETOOTH
}

enum class ConnectionStatus {
    DISCONNECTED,
    DISCOVERING,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class DiscoveredHost(
    val name: String,
    val ipAddress: String,
    val port: Int = 8888,
    val pin: String = "",
    val connectedPlayers: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)

data class ControllerPlayer(
    val id: Int,
    val name: String,
    val connectionType: ConnectionType,
    val ipOrAddress: String,
    val pingMs: Long = 0,
    val inputState: GamepadInputState = GamepadInputState()
)

/**
 * High-speed wire protocol encoder / decoder for Wi-Fi and Bluetooth.
 * Extremely low latency, zero overhead.
 */
object ProtocolSerializer {
    const val DEFAULT_PORT = 8888
    const val DISCOVERY_PORT = 8889
    const val DISCOVERY_MAGIC = "TV_GAMEPAD_DISCOVERY"
    const val DISCOVERY_ACK_PREFIX = "TV_GAMEPAD_HOST:"
    const val BT_UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB" // Standard SerialPort SPP

    fun encodeButton(button: GameButton, pressed: Boolean): String {
        return "B:${button.name}:${if (pressed) 1 else 0}\n"
    }

    fun encodeStick(x: Float, y: Float): String {
        return "S:%.2f:%.2f\n".format(x, y)
    }

    fun encodeRightStick(x: Float, y: Float): String {
        return "RS:%.2f:%.2f\n".format(x, y)
    }

    fun encodeTilt(tiltX: Float, tiltY: Float): String {
        return "T:%.2f:%.2f\n".format(tiltX, tiltY)
    }

    fun encodePing(time: Long): String {
        return "PING:$time\n"
    }

    fun encodePong(time: Long): String {
        return "PONG:$time\n"
    }

    fun encodeTriggers(l2: Float, r2: Float): String {
        return "TR:%.2f:%.2f\n".format(l2, r2)
    }

    fun encodeRemoteKey(key: TvRemoteKey, pressed: Boolean): String {
        return "RK:${key.name}:${if (pressed) 1 else 0}\n"
    }
}
