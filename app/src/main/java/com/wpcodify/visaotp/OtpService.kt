package com.wpcodify.visaotp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A tiny foreground service. Its only job is to keep the app's process alive so
 * Android does not kill it and miss an incoming OTP SMS. The actual SMS handling
 * is done by SmsReceiver; this service just holds a persistent notification.
 */
class OtpService : Service() {

    companion object {
        private const val CH = "visa_otp_fg"
        private const val NID = 42

        fun start(ctx: Context) {
            val i = Intent(ctx, OtpService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OtpService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CH, "OTP সংযোগ", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n: Notification = NotificationCompat.Builder(this, CH)
            .setContentTitle("Visa OTP Bridge চালু")
            .setContentText("OTP SMS-এর জন্য প্রস্তুত")
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NID, n)
        return START_STICKY // restart if the system kills us
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
