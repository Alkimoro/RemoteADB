package com.linked.remoteadb

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.Socket
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

object ConnectionManager {

    private val LOCK = Object()

    private val mutex = Mutex()

    private val socketListeningPause = AtomicBoolean(true)

    private val connecting = AtomicBoolean(false)
    @Volatile
    private var socket: SocketProto? = null

    private var connectState = ConnectState.INIT
    private val connectStateSubject = MutableSharedFlow<Pair<Boolean, ConnectState>>(replay = 0, extraBufferCapacity = 2)

    init {
        startSocketListening()
    }

    fun getConnectStateSubject(): SharedFlow<Pair<Boolean, ConnectState>> = connectStateSubject.asSharedFlow()

    fun connect(ip: String, port: Int, adbIp: String, adbPort: Int): Flow<ConnectState> {
        if (socket?.socket?.isConnected == true && AdbManager.isConnected()) {
            triggerSocketState(ConnectState.CONNECTED)
            return flowOf(ConnectState.CONNECTED)
        }
        if (!connecting.compareAndSet(false, true)) {
            triggerSocketState(ConnectState.CONNECTING)
            return flowOf(ConnectState.CONNECTING)
        }
        return flow {
            val result = try {
                if (AdbManager.connect(adbIp, adbPort)) {
                    synchronized(ConnectionManager) {
                        if (socket?.socket?.isConnected != true) {
                            close()
                            socket = SocketProto(Socket(ip, port))
                        }
                        socket?.sendMsg(data = ByteArray(0), type = "init", deviceId = UUID.randomUUID().toString())
                        if (socket?.socket?.isConnected == true) ConnectState.CONNECTED else ConnectState.CONNECT_FAIL
                    }
                } else ConnectState.ADB_FAIL
            } catch (e: Exception) {
                e.printStackTrace()
                ConnectState.CONNECT_FAIL
            }
            emit(result)
        }.flowOn(Dispatchers.IO).onEach {
            if (it == ConnectState.CONNECTED) {
                synchronized(LOCK) {
                    socketListeningPause.set(false)
                    LOCK.notifyAll()
                }
            }
            connecting.set(false)

            triggerSocketState(it)
        }
    }

    private fun startSocketListening() {
        Thread {
            while(true) {
                synchronized(LOCK) {
                    if (socketListeningPause.get()) {
                        LOCK.wait()
                    }
                }
                val tempSocket = socket
                if (tempSocket == null || socket?.socket?.isConnected != true) {
                    triggerSocketState(ConnectState.CONNECT_FAIL)
                    continue
                }

                val msg = tempSocket.getSocketMsgSafe()
                if (msg == null) {
                    triggerSocketState(ConnectState.CONNECT_FAIL)
                    continue
                }

                if (msg.type == "adb" && !AdbManager.sendAdbMsg(SocketType.ADB, msg.bytes)) {
                    triggerSocketState(ConnectState.ADB_FAIL)
                    continue
                }
                if (msg.type == "adb_custom" && !AdbManager.sendAdbMsg(
                        SocketType.CUSTOM,
                        msg.bytes
                    )
                ) {
                    triggerSocketState(ConnectState.ADB_FAIL)
                    continue
                }
                if (msg.type == "adb_reconnect_test_task" && !AdbManager.forceReConnect()) {
                    triggerSocketState(ConnectState.ADB_FAIL)
                    continue
                }
            }
        }.start()

        Thread {
            while(true) {
                synchronized(LOCK) {
                    if (socketListeningPause.get()) {
                        LOCK.wait()
                    }
                }
                val tempSocket = socket
                if (tempSocket == null || socket?.socket?.isConnected != true) {
                    triggerSocketState(ConnectState.CONNECT_FAIL)
                    continue
                }

                try {
                    val bytes = AdbManager.readBytes(SocketType.ADB)
                    if (bytes.isNotEmpty()) {
                        tempSocket.sendMsg(bytes, type = "adb")
                    }

                    val customBytes = AdbManager.readBytes(SocketType.CUSTOM)
                    if (customBytes.isNotEmpty()) {
                        tempSocket.sendMsg(customBytes, type = "adb_custom")
                    }
                } catch (e: Exception) {
                    triggerSocketState(ConnectState.ADB_FAIL)
                    continue
                }
            }
        }.start()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun triggerSocketState(reason: ConnectState) {
        when (reason) {
            ConnectState.CONNECT_FAIL -> {
                socketListeningPause.set(true)
                close()
            }
            ConnectState.ADB_FAIL -> {
                socketListeningPause.set(true)
                AdbManager.close()
            }
            else -> { }
        }

        GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
            mutex.withLock {
                val pre = connectState
                connectState = reason
                connectStateSubject.emit(Pair(reason != pre, reason))
            }
        }
    }

    fun close() {
        synchronized(ConnectionManager) {
            socket?.close()
            socket = null
        }
    }
}

enum class ConnectState {
    INIT,
    CONNECTING,
    CONNECTED,
    CONNECT_FAIL,
    ADB_FAIL,
}