package com.linked.remoteadb

import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class SocketCollection {

    private val socketMap = ConcurrentHashMap<SocketType, Socket>()
    @Volatile
    private var host: String? = null
    @Volatile
    private var port: Int? = null

    @Synchronized
    fun connect(host: String, port: Int): Boolean {
        this.host = host
        this.port = port
        if (isConnected()) {
            return true
        }
        return forceReConnect()
    }

    @Synchronized
    private fun forceReConnect(): Boolean {
        val curHost = this.host ?: return false
        val curPort = this.port ?: return false
        closeAll()
        SocketType.entries.forEach {
            try {
                val socket = Socket(curHost, curPort)
                if (!socket.isConnected) {
                    socket.release()
                    return false
                }
                socketMap.put(it, socket)
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return true
    }

    @Synchronized
    fun readBytes(type: SocketType): ByteArray {
        val result = socketMap.get(type)?.getInputStream()?.let {
            if (it.available() == 0) {
                ByteArray(0)
            } else {
                val array = ByteArray(it.available())
                val count = it.read(array)
                when {
                    count == array.size -> array
                    count == 0 -> ByteArray(0)
                    else -> {
                        array.copyOfRange(0, count)
                    }
                }
            }
        } ?: throw Exception("Adb connect fail")
        return result
    }

    //仅供参考，不是很准确
    @Synchronized
    fun isConnected(): Boolean {
        return socketMap.size == SocketType.entries.size && socketMap.all { it.value.isConnected }
    }

    @Synchronized
    fun sendAdbMsg(type: SocketType, data: ByteArray, retryCount: Int = 1): Boolean {
        if (data.isEmpty()) return true
        return try {
            val stream = socketMap.get(type)?.getOutputStream() ?: return false
            stream.write(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            if (retryCount >= 1) {
                forceReConnect() && sendAdbMsg(type, data, retryCount - 1)
            } else {
                false
            }
        }
    }

    @Synchronized
    fun closeAll() {
        socketMap.forEach {
            close(it.key)
        }
    }

    @Synchronized
    private fun close(type: SocketType) {
        socketMap.get(type).release()
        socketMap.remove(type)
    }

}

enum class SocketType {
    ADB,
    CUSTOM,
}

fun Socket?.release() {
    this ?: return
    runCatching { this.shutdownInput() }
    runCatching { this.shutdownOutput() }
    runCatching { this.close() }
}