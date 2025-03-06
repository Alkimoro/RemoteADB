package com.linked.remoteadb.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.linked.remoteadb.AdbManager
import com.linked.remoteadb.ConnectionManager

/**
 * 用于保活，不然创建的tcp socket在app退到后台一段时间后会自动关闭
 * */
class ConnectionService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        crateChannel()
        startForeground()
        return START_STICKY
    }

    private fun startForeground() {
        let {
            ServiceCompat.startForeground(
                this,
                123456,
                NotificationCompat.Builder(this.applicationContext, "remote_adb")
                    .setContentTitle("RemoteADB运行中")
                    .setContentText("RemoteADB运行中")
                    .build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                } else { 0 },
            )
        }
    }

    private fun crateChannel() {
        runCatching {
            val notification = NotificationChannel(
                "remote_adb",
                "remote_adb",
                NotificationManager.IMPORTANCE_HIGH
            )
            notification.description = "remote_adb"
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdbManager.close()
        ConnectionManager.close()
    }
}