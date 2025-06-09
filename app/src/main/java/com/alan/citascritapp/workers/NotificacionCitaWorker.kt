package com.alan.citascritapp.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.alan.citascritapp.R
import android.app.PendingIntent
import android.content.Intent
import com.alan.citascritapp.MainActivity

class NotificacionCitaWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val titulo = inputData.getString("titulo") ?: inputData.getString("title") ?: "Cita CRIT"
        val mensaje = inputData.getString("mensaje") ?: inputData.getString("text") ?: "Tienes una cita próximamente."
        val notificationId = inputData.getInt("notificationId", System.currentTimeMillis().toInt())
        mostrarNotificacion(applicationContext, titulo, mensaje, notificationId)
        return Result.success()
    }

    private fun mostrarNotificacion(context: Context, titulo: String, mensaje: String, notificationId: Int) {
        val channelId = "citas_crit_channel_v4"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.police_woop_woop}")

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                channelId,
                "Citas CRIT",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de citas próximas"
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 300)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_dino_notif)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 300))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }
}
