package com.linked.remoteadb

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConnectionViewModel : ViewModel() {

    @Composable
    fun getConnectState() = ConnectionManager.getConnectStateSubject()
        .filter { it.first }
        .map { it.second }
        .collectAsState(ConnectState.INIT)

    fun init(): ConnectionViewModel {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                ConnectionManager.getConnectStateSubject().collect {
                    val text = when (it.second) {
                        ConnectState.CONNECTED -> "连接成功"
                        ConnectState.CONNECT_FAIL -> "ADB服务器连接失败"
                        ConnectState.ADB_FAIL -> "本地ADB服务器创建失败，请检查是否开启ADB无线调试"
                        ConnectState.CONNECTING -> "连接中"
                        else -> ""
                    }
                    if (text.isNotEmpty()) {
                        Toast.makeText(MainActivity.mainActivity, text, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    fun connect(ip: String, port: Int, adbIp: String, adbPort: Int, proxyIp: String, proxyPort: Int) {
        viewModelScope.launch {
            ConnectionManager.connect(ip, port, adbIp, adbPort, proxyIp, proxyPort).first()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

}