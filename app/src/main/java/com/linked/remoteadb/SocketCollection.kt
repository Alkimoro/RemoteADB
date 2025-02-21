package com.linked.remoteadb

import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

class SocketCollection {

    private val socketMap = ConcurrentHashMap<SocketType, Socket>()

    @Synchronized
    fun connect(host: String, port: Int): Boolean {
        if (isConnected()) {
            return true
        }
        closeAll()
        SocketType.entries.forEach {
            try {
                val socket = Socket(host, port)
                if (!socket.isConnected) {
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

    @Synchronized
    fun isConnected(): Boolean {
        return socketMap.size == SocketType.entries.size && socketMap.all { it.value.isConnected }
    }

    @Synchronized
    fun sendAdbMsg(type: SocketType, data: ByteArray): Boolean {
        if (data.isEmpty()) return true
        return try {
            val stream = socketMap.get(type)?.getOutputStream() ?: return false
            stream.write(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
        try {
            val socket = socketMap.get(type)
            socket?.shutdownInput()
            socket?.shutdownOutput()
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            socketMap.remove(type)
        }
    }

}

enum class SocketType {
    ADB,
    CUSTOM,
}