package com.linked.remoteadb.utils

import java.io.InputStream

fun InputStream.readCountBytes(length: Int): ByteArray {
    val array = ByteArray(length)
    var count = 0

    while (count < length) {
        val currentRead = this.read(array, count, length - count)
        if (currentRead < 0) {
            throw Exception("can not read more bytes")
        }
        count += currentRead
    }
    return array
}