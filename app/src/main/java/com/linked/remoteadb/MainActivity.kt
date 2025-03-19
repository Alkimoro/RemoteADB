package com.linked.remoteadb

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.linked.remoteadb.service.ConnectionService
import com.linked.remoteadb.stun.StunClient
import com.linked.remoteadb.ui.theme.RemoteADBTheme

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var mainActivity: MainActivity
    }

    private val viewModel by lazy { ViewModelProvider(this)[ConnectionViewModel::class.java].init() }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivity = this
        enableEdgeToEdge()
        setContent {
            RemoteADBTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            colors = topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            title = {
                                Text("RemoteADB")
                            }
                        )
                    },
                    content = { paddingValues ->
                        Column(modifier = Modifier.padding(paddingValues)) {
                            Text(
                                "连接ADB服务器",
                                style = TextStyle(fontSize = 24.sp),
                                modifier = Modifier.padding(20.dp)
                            )

                            var ipText by remember { mutableStateOf("18.166.201.88") }
                            TextField(
                                modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 10.dp).fillMaxWidth(),
                                value = ipText,
                                onValueChange = { ipText = it },
                                label = { Text("请输入服务器ip") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                            )

                            var ipPort by remember { mutableStateOf("12346") }
                            TextField(
                                modifier = Modifier.padding(20.dp, 0.dp).fillMaxWidth(),
                                value = ipPort,
                                onValueChange = { ipPort = it },
                                label = { Text("请输入服务器端口") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(20.dp, 20.dp).fillMaxWidth(),
                                thickness = .5.dp
                            )

                            var adbIpText by remember { mutableStateOf("localhost") }
                            TextField(
                                modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 10.dp).fillMaxWidth(),
                                value = adbIpText,
                                onValueChange = { adbIpText = it },
                                label = { Text("请输入adb无线调试中显示的ip") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                            )

                            var adbPort by remember { mutableStateOf("5037") }
                            TextField(
                                modifier = Modifier.padding(20.dp, 0.dp).fillMaxWidth(),
                                value = adbPort,
                                onValueChange = { adbPort = it },
                                label = { Text("请输入adb无线调试中显示的端口") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Button(
                                modifier = Modifier.fillMaxWidth().padding(20.dp, 10.dp),
                                onClick = {
                                    StunClient.startRequest {
                                        println("====${it}")
                                    }
                                    val intent = Intent(this@MainActivity, ConnectionService::class.java)
                                    this@MainActivity.startForegroundService(intent)
                                    viewModel.connect(
                                        ipText,
                                        ipPort.toIntOrNull() ?: 12346,
                                        adbIpText,
                                        adbPort.toIntOrNull() ?: 5037
                                    )
                                }
                            ) {
                                Text(
                                    style = TextStyle(fontSize = 16.sp),
                                    text = "连接到服务器",
                                )
                            }

                            Text(
                                "设备状态:",
                                style = TextStyle(fontSize = 18.sp),
                                modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 10.dp)
                            )

                            val connected by viewModel.getConnectState()
                            FilterChip(
                                modifier = Modifier.padding(20.dp, 0.dp),
                                label = {
                                    Text(if (connected == ConnectState.CONNECTED) "已连接" else "未连接")
                                },
                                selected = connected == ConnectState.CONNECTED,
                                onClick = { },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (connected == ConnectState.CONNECTED) Icons.Filled.Done else Icons.Filled.Close,
                                        contentDescription = "icon",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            )

                            Text(
                                "设备信息:\nModel: ${Build.MODEL}\nVersion: ${Build.VERSION.SDK_INT}\n品牌: ${Build.BRAND}\n硬件名: ${Build.HARDWARE}",
                                style = TextStyle(fontSize = 18.sp),
                                modifier = Modifier.padding(20.dp, 20.dp, 20.dp, 10.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val c = remember { mutableStateOf("") }
    Text(
        text = "Hello $c!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RemoteADBTheme {
        Greeting("Android")
    }
}