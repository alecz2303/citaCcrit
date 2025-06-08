package com.alan.citascritapp.broadcasts

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.net.Uri
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alan.citascritapp.MainActivity
import com.alan.citascritapp.R

class AlarmaReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val tipo = intent.getStringExtra("tipo")

        if (tipo == "alerta_citas_dia_siguiente") {
            val fecha = intent.getStringExtra("fecha") ?: ""
            val mensaje = "¡Mañana tienes una o más citas en el CRIT ($fecha)!"
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.police_woop_woop}")

            val builder = NotificationCompat.Builder(context, "citas_channel_v3")
                .setSmallIcon(R.drawable.ic_dino_notif)
                .setContentTitle("Citas CRIT mañana")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(longArrayOf(0, 200, 100, 300))

            NotificationManagerCompat.from(context)
                .notify(("alerta_" + fecha).hashCode(), builder.build())
            Log.d("ALERTA_MAÑANA", "Notificación enviada: $mensaje")
            return
        }

        // Notificación normal de cita
        val servicio = intent.getStringExtra("servicio") ?: "Cita CRIT"
        val hora = intent.getStringExtra("hora") ?: ""
        val fecha = intent.getStringExtra("fecha") ?: ""
        val mensaje = "¡Tienes una cita de $servicio a las $hora!"
        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.police_woop_woop}")

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "citas_channel_v3")
            .setSmallIcon(R.drawable.ic_dino_notif)
            .setContentTitle("Recordatorio de cita")
            .setContentText(mensaje)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 200, 100, 300))
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(context).notify(
            (servicio + hora + fecha).hashCode(), builder.build()
        )

        Log.d("ALARMA_RECEIVER", "Notificación mostrada: $mensaje")
    }
}
