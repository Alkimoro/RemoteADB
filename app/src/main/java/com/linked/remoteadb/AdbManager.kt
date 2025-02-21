package com.linked.remoteadb

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader


object AdbManager {

    private val TAG = "AdbManager"

//    private val host = "localhost"
//    private val port = 5037

    private val socket = SocketCollection()

    fun connect(host: String, port: Int): Boolean {
        return socket.connect(host, port)
    }

    fun isConnected(): Boolean {
        return socket.isConnected()
    }

    fun readBytes(type: SocketType): ByteArray {
        return socket.readBytes(type)
    }

    fun close() {
        socket.closeAll()
    }

    fun sendAdbMsg(type: SocketType, data: ByteArray): Boolean {
        return socket.sendAdbMsg(type, data)
    }

    fun adb(command: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                Log.d(TAG, line ?: "")
            }
            process.waitFor()
            reader.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}