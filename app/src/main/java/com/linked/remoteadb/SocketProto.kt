package com.linked.remoteadb

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.linked.remoteadb.model.socket.SocketMsg
import com.linked.remoteadb.utils.readCountBytes
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

const val CHUNK_HEAD_SIZE = 8

class SocketProto(val socket: Socket) {
    companion object {
        const val TAG = "SocketProto"
    }

    fun getSocketMsg(): SocketMsg {
        LazyLogger.d(TAG) { "getSocketMsg: --${this.socket.getInputStream().available()}" }
        val bytes = this.socket.getInputStream().readCountBytes(CHUNK_HEAD_SIZE)
        val len = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong().toInt()
        LazyLogger.d(TAG) { "getSocketMsg: --len:${len}, bytes:${bytes.size}" }

        val msgBytes = this.socket.getInputStream().readCountBytes(len)

        val jsonString = String(msgBytes, Charsets.UTF_8)
        LazyLogger.d(TAG) { "getSocketMsg:${jsonString}" }
        val element = JsonParser().parse(jsonString)
        val bytesLength = (element as? JsonObject)?.getAsJsonPrimitive("bytes_length")?.asInt ?: 0
        val payload = if (bytesLength > 0) {
            this.socket.getInputStream().readCountBytes(bytesLength)
        } else {
            ByteArray(0)
        }
        return SocketMsg.fromJson(element, payload)
    }

    fun getSocketMsgSafe(): SocketMsg? {
        return try {
            getSocketMsg()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sendMsg(data: ByteArray, type: String, deviceId: String = "", msg: String = "") {
        val socketMsg = SocketMsg(
            type = type,
            msg = msg,
            device_id = deviceId,
            bytes_length = data.size,
            bytes = data
        )
        val msgBytes = socketMsg.decodeToBytes()
        LazyLogger.d(TAG) { "sendAdbMsg:${msgBytes.size}, ${socketMsg}" }
        socket.getOutputStream().write(
            intToBytes(msgBytes.size.toLong()) + msgBytes + socketMsg.bytes
        )
    }

    fun intToBytes(value: Long): ByteArray {
        val buffer = ByteBuffer.allocate(8)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putLong(value)
        return buffer.array()
    }

    fun close() {
        socket.release()
    }
}