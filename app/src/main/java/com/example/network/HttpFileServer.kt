package com.example.network

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

data class SharedFileInfo(
    val id: String = UUID.randomUUID().toString().take(8),
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val addedTime: Long = System.currentTimeMillis()
) {
    val readableSize: String
        get() = when {
            sizeBytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(sizeBytes / (1024f * 1024f * 1024f))
            sizeBytes >= 1024 * 1024 -> "%.1f MB".format(sizeBytes / (1024f * 1024f))
            sizeBytes >= 1024 -> "%.1f KB".format(sizeBytes / 1024f)
            else -> "$sizeBytes B"
        }
}

class HttpFileServer(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "HttpFileServer"
        const val SERVER_PORT = 8080
    }

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    private val _sharedFiles = MutableStateFlow<List<SharedFileInfo>>(emptyList())
    val sharedFiles: StateFlow<List<SharedFileInfo>> = _sharedFiles.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _transferLog = MutableStateFlow("سرور انتقال فایل آماده است")
    val transferLog: StateFlow<String> = _transferLog.asStateFlow()

    fun startServer() {
        if (_isRunning.value) return

        val localIp = NetworkUtils.getLocalIpAddress()
        val url = "http://$localIp:$SERVER_PORT"
        _serverUrl.value = url

        serverJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(SERVER_PORT))
                serverSocket = server
                _isRunning.value = true
                _transferLog.value = "سرور در آدرس $url فعال شد"
                Log.d(TAG, "File server running on $url")

                while (isActive && !server.isClosed) {
                    val clientSocket = try {
                        server.accept()
                    } catch (e: Exception) {
                        break
                    }
                    launch(Dispatchers.IO) {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
                _transferLog.value = "خطا در شروع سرور: ${e.localizedMessage}"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null
        _isRunning.value = false
        _transferLog.value = "سرور متوقف شد"
    }

    fun addFile(uri: Uri): SharedFileInfo? {
        val cr = context.contentResolver
        var fileName = "file_${System.currentTimeMillis()}"
        var fileSize = 0L

        try {
            cr.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving URI metadata", e)
        }

        val mimeType = cr.getType(uri) ?: "application/octet-stream"
        val info = SharedFileInfo(
            uri = uri,
            name = fileName,
            sizeBytes = fileSize,
            mimeType = mimeType
        )

        val list = _sharedFiles.value.toMutableList()
        list.add(0, info)
        _sharedFiles.value = list
        _transferLog.value = "فایل «$fileName» آماده دریافت در تلویزیون است"

        if (!_isRunning.value) {
            startServer()
        }
        return info
    }

    fun removeFile(id: String) {
        val list = _sharedFiles.value.toMutableList()
        list.removeAll { it.id == id }
        _sharedFiles.value = list
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 15000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = BufferedOutputStream(socket.getOutputStream())

            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = URLDecoder.decode(parts[1], "UTF-8")

            if (method.equals("GET", ignoreCase = true)) {
                when {
                    path == "/" || path.startsWith("/?") -> {
                        sendHtmlPortal(out)
                    }
                    path.startsWith("/download/") -> {
                        val fileId = path.removePrefix("/download/").substringBefore("?").substringBefore("/")
                        val file = _sharedFiles.value.find { it.id == fileId }
                        if (file != null) {
                            sendFile(file, out)
                        } else {
                            send404(out)
                        }
                    }
                    else -> send404(out)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Client handle exception: ${e.message}")
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    private fun sendHtmlPortal(out: BufferedOutputStream) {
        val files = _sharedFiles.value
        val itemsHtml = if (files.isEmpty()) {
            """
            <div class="empty-state">
                <p>هیچ فایلی برای دانلود انتخاب نشده است.</p>
                <small>در برنامه گوشی، دکمه «انتخاب و ارسال فایل» را بزنید.</small>
            </div>
            """.trimIndent()
        } else {
            files.joinToString("\n") { file ->
                val encodedName = URLEncoder.encode(file.name, "UTF-8")
                val isApk = file.name.endsWith(".apk", ignoreCase = true)
                val badge = if (isApk) "<span class='badge apk'>برنامه APK</span>" else "<span class='badge'>فایل</span>"
                """
                <div class="file-card">
                    <div class="file-meta">
                        <div class="file-name">${file.name}</div>
                        <div class="file-details">$badge • اندازه: ${file.readableSize}</div>
                    </div>
                    <a href="/download/${file.id}/$encodedName" class="btn-download" download="${file.name}">
                        ⬇️ دریافت / نصب روی تلویزیون
                    </a>
                </div>
                """.trimIndent()
            }
        }

        val html = """
            <!DOCTYPE html>
            <html lang="fa" dir="rtl">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>ارسال فایل به تلویزیون • گیم‌پد و ریموت</title>
                <style>
                    body {
                        background-color: #0b111e;
                        color: #f8fafc;
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Tahoma, sans-serif;
                        margin: 0;
                        padding: 24px;
                        display: flex;
                        justify-content: center;
                    }
                    .container {
                        max-width: 680px;
                        width: 100%;
                    }
                    .header {
                        text-align: center;
                        padding: 20px 0;
                        border-bottom: 1px solid #1e293b;
                        margin-bottom: 24px;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 24px;
                        color: #00f0ff;
                    }
                    .header p {
                        color: #94a3b8;
                        font-size: 14px;
                        margin-top: 8px;
                    }
                    .file-card {
                        background: #151f32;
                        border: 1px solid #233554;
                        border-radius: 14px;
                        padding: 16px 20px;
                        margin-bottom: 14px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .file-meta {
                        flex: 1;
                        padding-left: 12px;
                    }
                    .file-name {
                        font-size: 16px;
                        font-weight: bold;
                        word-break: break-all;
                        color: #fff;
                    }
                    .file-details {
                        font-size: 13px;
                        color: #94a3b8;
                        margin-top: 4px;
                    }
                    .badge {
                        background: #1e2d48;
                        color: #00f0ff;
                        padding: 2px 8px;
                        border-radius: 6px;
                        font-size: 11px;
                    }
                    .badge.apk {
                        background: #064e3b;
                        color: #10b981;
                    }
                    .btn-download {
                        background: #0284c7;
                        color: #fff;
                        text-decoration: none;
                        padding: 10px 18px;
                        border-radius: 10px;
                        font-weight: bold;
                        font-size: 14px;
                        white-space: nowrap;
                        transition: 0.2s;
                    }
                    .btn-download:hover {
                        background: #0ea5e9;
                    }
                    .empty-state {
                        text-align: center;
                        padding: 40px;
                        background: #121b2d;
                        border-radius: 12px;
                        color: #94a3b8;
                    }
                    .instructions {
                        margin-top: 24px;
                        padding: 16px;
                        background: #0f172a;
                        border-radius: 12px;
                        border: 1px dashed #334155;
                        font-size: 13px;
                        color: #cbd5e1;
                        line-height: 1.8;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📺 ارسال فایل به تلویزیون هوشمند</h1>
                        <p>فایل‌های ارسال شده از طریق گوشی آماده دانلود در تلویزیون هستند</p>
                    </div>
                    $itemsHtml
                    <div class="instructions">
                        💡 <strong>راهنما برای تلویزیون:</strong><br/>
                        ۱. اگر فایل دانلود شده یک برنامه (APK) است، پس از اتمام دانلود روی آن کلیک کنید تا در تلویزیون نصب شود.<br/>
                        ۲. می‌توانید از مرورگرهای تلویزیون مثل TV Bro, Chrome یا اپلیکیشن Downloader استفاده کنید.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()

        val bytes = html.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun sendFile(file: SharedFileInfo, out: BufferedOutputStream) {
        val inputStream = context.contentResolver.openInputStream(file.uri)
        if (inputStream == null) {
            send404(out)
            return
        }

        try {
            val encodedName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: ${file.mimeType}\r\n" +
                    "Content-Length: ${file.sizeBytes}\r\n" +
                    "Content-Disposition: attachment; filename=\"${file.name}\"; filename*=UTF-8''$encodedName\r\n" +
                    "Accept-Ranges: bytes\r\n" +
                    "Connection: close\r\n\r\n"
            out.write(header.toByteArray(Charsets.UTF_8))

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
            out.flush()
            _transferLog.value = "فایل «${file.name}» با موفقیت توسط تلویزیون دانلود شد."
        } finally {
            inputStream.close()
        }
    }

    private fun send404(out: BufferedOutputStream) {
        val msg = "404 Not Found"
        val header = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: ${msg.length}\r\n" +
                "Connection: close\r\n\r\n" + msg
        out.write(header.toByteArray(Charsets.UTF_8))
        out.flush()
    }

    suspend fun generateQrCodeBitmap(text: String, size: Int = 512): Bitmap = withContext(Dispatchers.Default) {
        val bitMatrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    }
}
