package com.celltracker.scanner

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.celltracker.R
import com.celltracker.ui.MainActivity

class ScannerService : Service() {

    companion object {
        const val CHANNEL_ID     = "cell_scanner_channel"
        const val NOTIFICATION_ID = 1001
        const val SCAN_INTERVAL_MS = 5_000L
        const val ACTION_START = "com.celltracker.START_SCAN"
        const val ACTION_STOP  = "com.celltracker.STOP_SCAN"
    }

    private lateinit var scanner: CellScanner
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false

    private val scanRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                val towers = scanner.scan()
                updateNotification(towers.size, towers.firstOrNull()?.signalStrength ?: -999)
                handler.postDelayed(this, SCAN_INTERVAL_MS)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        scanner = CellScanner(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startScanning()
            ACTION_STOP  -> stopScanning()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startScanning() {
        isScanning = true
        startForeground(NOTIFICATION_ID, buildNotification(0, -999))
        handler.post(scanRunnable)
    }

    private fun stopScanning() {
        isScanning = false
        handler.removeCallbacks(scanRunnable)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(towerCount: Int, dbm: Int) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(towerCount, dbm))
    }

    private fun buildNotification(towerCount: Int, dbm: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val dbmText = if (dbm > -999) "$dbm dBm" else "Scanning…"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cell Tracker Active")
            .setContentText("Signal: $dbmText · $towerCount tower${if (towerCount == 1) "" else "s"} visible")
            .setSmallIcon(R.drawable.ic_signal)
            .setContentIntent(pi)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cell Scanner",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background cell tower scanning"
                setShowBadge(false)
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isScanning = false
        handler.removeCallbacks(scanRunnable)
        super.onDestroy()
    }
}
