package com.linked.remoteadb.model.socket

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.Expose

val gson = Gson()

class SocketMsg(
    val type: String = "",
    val msg: String = "",
    val device_id: String = "",
    val bytes_length: Int = 0,
    @Transient
    @Expose(serialize = false, deserialize = false)
    val bytes: ByteArray = ByteArray(0),
) {
    companion object {
        // fast
        fun fromJson(element: JsonElement, bytes: ByteArray): SocketMsg {
            if (element !is JsonObject) return SocketMsg()
            return SocketMsg(
                type = element.getAsJsonPrimitive("type")?.asString ?: "",
                msg = element.getAsJsonPrimitive("msg")?.asString ?: "",
                device_id = element.getAsJsonPrimitive("device_id")?.asString ?: "",
                bytes_length = bytes.size,
                bytes = bytes
            )
        }
    }

    override fun toString(): String {
        return "type:${type},bytes_length:${bytes_length}"
    }

    fun decodeToBytes(): ByteArray {
        return gson.toJson(this).toByteArray(Charsets.UTF_8)
    }

}