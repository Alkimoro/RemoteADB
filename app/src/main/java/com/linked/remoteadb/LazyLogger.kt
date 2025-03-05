package com.linked.remoteadb

import android.util.Log

object LazyLogger {
    private val enable = false

    fun d(tag: String, callback: () -> String) {
        ensureEnable {
            Log.d(tag, callback.invoke())
        }
    }


    private inline fun ensureEnable(callback: () -> Unit) {
        if (!enable) return
        callback.invoke()
    }
}