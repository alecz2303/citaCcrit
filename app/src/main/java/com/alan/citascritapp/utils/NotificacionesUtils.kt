package com.alan.citascritapp.utils

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.*
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.workers.NotificacionCitaWorker
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.O)
fun programarNotificacionesCitas(context: Context, citas: List<Cita>) {
    WorkManager.getInstance(context).cancelAllWorkByTag("cita_notif")

    citas.forEachIndexed { index, cita ->
        try {
            val fechaNoti = calcularFechaHoraCita(cita.fecha, cita.hora)?.minusMinutes(10)
            if (fechaNoti != null && fechaNoti.isAfter(LocalDateTime.now())) {
                val delay = Duration.between(LocalDateTime.now(), fechaNoti).toMillis()
                val data = Data.Builder()
                    .putString("title", "Cita en 10 minutos")
                    .putString("text", "${cita.servicio} con ${cita.medico} a las ${cita.hora}")
                    .putInt("notificationId", 1000 + index)
                    .build()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun programarNotificacionCita(context: Context, cita: Cita) {
    val diasEsEn = mapOf(
        "lunes" to "Monday", "martes" to "Tuesday", "miércoles" to "Wednesday", "jueves" to "Thursday",
        "viernes" to "Friday", "sábado" to "Saturday", "domingo" to "Sunday"
    )
    val mesesEsEn = mapOf(
        "enero" to "January", "febrero" to "February", "marzo" to "March", "abril" to "April",
        "mayo" to "May", "junio" to "June", "julio" to "July", "agosto" to "August",
        "septiembre" to "September", "octubre" to "October", "noviembre" to "November", "diciembre" to "December"
    )
    var input = "${cita.fecha} ${cita.hora}"
        .replace(".", "")
        .replace("  ", " ")
        .trim()
        .lowercase()
    for ((es, en) in diasEsEn) {
        if (input.startsWith(es)) {
            input = input.replaceFirst(es, en)
            break
        }
    }
    for ((es, en) in mesesEsEn) {
        input = input.replace(" de $es de ", " of $en of ")
    }
    val formato = SimpleDateFormat("EEEE, dd 'of' MMMM 'of' yyyy hh:mm a", Locale.ENGLISH)
    val fechaHora: Date? = try { formato.parse(input) } catch (e: Exception) { null }
    if (fechaHora == null) return

    val tiempoNotificacion = fechaHora.time - (10 * 60 * 1000) // 10 minutos antes
    val delay = tiempoNotificacion - System.currentTimeMillis()
    if (delay <= 0) return // Ya pasó

    val data = Data.Builder()
        .putString("titulo", "Cita CRIT")
        .putString("mensaje", "¡Recuerda tu cita de ${cita.servicio} a las ${cita.hora}!")
        .build()
    val tag = "cita_${cita.fecha}_${cita.hora}_${cita.servicio}"
    val workRequest = OneTimeWorkRequestBuilder<NotificacionCitaWorker>()
        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
        .setInputData(data)
        .addTag(tag)
        .build()
    WorkManager.getInstance(context).enqueue(workRequest)
}

fun cancelarTodasNotificacionesCitas(context: Context) {
    WorkManager.getInstance(context).cancelAllWork()
}
