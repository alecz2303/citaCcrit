package com.alan.citascritapp.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.broadcasts.AlarmaReceiver
import com.alan.citascritapp.R
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

fun agregarAlarmaDebug(context: Context, descripcion: String) {
    val prefs = context.getSharedPreferences("alarmas_debug", Context.MODE_PRIVATE)
    val set = prefs.getStringSet("alarmas", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    set.add(descripcion)
    prefs.edit().putStringSet("alarmas", set).apply()
}

fun obtenerAlarmasDebug(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("alarmas_debug", Context.MODE_PRIVATE)
    return prefs.getStringSet("alarmas", emptySet()) ?: emptySet()
}

fun limpiarAlarmasDebug(context: Context) {
    val prefs = context.getSharedPreferences("alarmas_debug", Context.MODE_PRIVATE)
    prefs.edit().remove("alarmas").apply()
}

fun programarAlarmaCita(
    context: Context,
    cita: Cita,
    onPermisoFaltante: (() -> Unit)? = null // Nuevo parámetro para callback
) {
    val tag = "cita_${cita.fecha}_${cita.hora}_${cita.servicio}"
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val triggerAtMillis = getCitaTriggerTimeMillis(cita)
    Log.d("ALARMA_DEBUG", "Calculado triggerAtMillis: $triggerAtMillis (${cita.servicio} ${cita.fecha} ${cita.hora})")
    if (triggerAtMillis == null || triggerAtMillis <= System.currentTimeMillis()) {
        Log.d("ALARMA_DEBUG", "No se programa la alarma para $tag (fecha pasada o error)")
        return
    }
    val pendingIntent = crearPendingIntentCita(context, cita)
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.d("ALARMA_DEBUG", "Alarma EXACTA programada (Android 12+)")
            } else {
                // *** AQUÍ ya NO mostramos Toast ni lanzamos el intent ***
                Log.e("ALARMA_DEBUG", "No tiene permiso para alarmas exactas en Android 12+")
                onPermisoFaltante?.invoke() // Llama al callback para mostrar el diálogo
                return
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
            Log.d("ALARMA_DEBUG", "Alarma programada para versión menor a Android 12")
        }
    } catch (e: Exception) {
        Log.e("ALARMA_DEBUG", "Error inesperado al programar alarma: ${e.message}")
    }
    // Registro debug
    agregarAlarmaDebug(
        context,
        "Cita: ${cita.servicio} - ${cita.fecha} ${cita.hora} (id: $tag)"
    )
}

fun cancelarAlarmaCita(context: Context, cita: Cita) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = crearPendingIntentCita(context, cita)
    alarmManager.cancel(pendingIntent)
    Log.d("ALARMA_DEBUG", "Alarma cancelada para cita: ${cita.servicio} ${cita.fecha} ${cita.hora}")
}

suspend fun cancelarTodasAlarmasCitas(context: Context) {
    val citas = cargarCitas(context)
    for (cita in citas) {
        cancelarAlarmaCita(context, cita)
    }
    limpiarAlarmasDebug(context)
}

fun crearPendingIntentCita(context: Context, cita: Cita): PendingIntent {
    val intent = Intent(context, AlarmaReceiver::class.java).apply {
        putExtra("servicio", cita.servicio)
        putExtra("hora", cita.hora)
        putExtra("fecha", cita.fecha)
    }
    val requestCode = cita.hashCode()
    return PendingIntent.getBroadcast(
        context,
        requestCode,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

fun getCitaTriggerTimeMillis(cita: Cita): Long? {
    // Une fecha y hora, elimina puntos en am/pm
    var input = "${cita.fecha} ${cita.hora}"
        .replace(".", "")
        .replace("  ", " ")
        .trim()
        .lowercase()

    // Diccionarios de traducción
    val diasEsEn = mapOf(
        "lunes" to "Monday", "martes" to "Tuesday", "miércoles" to "Wednesday", "jueves" to "Thursday",
        "viernes" to "Friday", "sábado" to "Saturday", "domingo" to "Sunday"
    )
    val mesesEsEn = mapOf(
        "enero" to "January", "febrero" to "February", "marzo" to "March", "abril" to "April",
        "mayo" to "May", "junio" to "June", "julio" to "July", "agosto" to "August",
        "septiembre" to "September", "octubre" to "October", "noviembre" to "November", "diciembre" to "December"
    )

    // Cambia día a inglés
    for ((es, en) in diasEsEn) {
        if (input.startsWith(es)) {
            input = input.replaceFirst(es, en)
            break
        }
    }
    // Cambia mes a inglés
    for ((es, en) in mesesEsEn) {
        input = input.replace(" de $es de ", " of $en of ")
    }
    // Reemplaza "a m"/"p m" a AM/PM
    input = input.replace(" a m", " AM").replace(" p m", " PM")

    // El formato debe coincidir con el string:
    // Ejemplo: Thursday, 12 of June of 2025 10:00 AM
    val formato = SimpleDateFormat("EEEE, dd 'of' MMMM 'of' yyyy hh:mm a", Locale.ENGLISH)

    return try {
        val fecha = formato.parse(input)
        if (fecha != null) {
            // Devuelve 10 minutos antes
            fecha.time - (10 * 60 * 1000)
        } else {
            Log.e("ALARMA_DEBUG", "No se pudo parsear fecha para la cita: $input")
            null
        }
    } catch (e: Exception) {
        Log.e("ALARMA_DEBUG", "Error parseando fecha: $input | ${e.message}")
        null
    }
}


// ----------- ALERTA DE CITAS MAÑANA A LAS 8PM ------------

/**
 * Programa una alarma para las 8:00pm del día anterior a cada día con cita.
 */
fun programarAlertasCitasDiaSiguiente(context: Context, citas: List<Cita>) {
    cancelarAlertasCitasDiaSiguiente(context, citas)

    val fechasConCitas = citas.filter { !it.cancelada }
        .map { it.fecha }
        .toSet()

    val formato = SimpleDateFormat("EEEE, dd 'de' MMMM 'de' yyyy", Locale("es", "MX"))

    for (fecha in fechasConCitas) {
        try {
            val date = formato.parse(fecha) ?: continue
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.DATE, -1) // un día antes
            cal.set(Calendar.HOUR_OF_DAY, 10) // 8pm
            cal.set(Calendar.MINUTE, 1)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            if (cal.timeInMillis <= System.currentTimeMillis()) continue

            val requestCode = ("alerta_$fecha").hashCode()
            val intent = Intent(context, AlarmaReceiver::class.java).apply {
                putExtra("tipo", "alerta_citas_dia_siguiente")
                putExtra("fecha", fecha)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
            Log.d("ALERTA_MAÑANA", "Programada alerta para $fecha, el ${cal.time}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * Cancela las alertas del tipo "mañana tienes citas".
 */
fun cancelarAlertasCitasDiaSiguiente(context: Context, citas: List<Cita>) {
    val fechasConCitas = citas.map { it.fecha }.toSet()
    for (fecha in fechasConCitas) {
        val requestCode = ("alerta_$fecha").hashCode()
        val intent = Intent(context, AlarmaReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}

fun crearCanalNotificaciones(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channelId = "citas_crit_channel_v4"
        val name = "Citas CRIT"
        val descriptionText = "Recordatorios de citas"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 300)
            val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.police_woop_woop}")
            setSound(soundUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}
