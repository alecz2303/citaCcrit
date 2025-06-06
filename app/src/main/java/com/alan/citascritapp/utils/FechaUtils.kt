package com.alan.citascritapp.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale



fun String.toEdad(): Int? = try {
    val nacimiento = org.threeten.bp.LocalDate.parse(this)
    val ahora = org.threeten.bp.LocalDate.now()
    var edad = ahora.year - nacimiento.year
    if (ahora < nacimiento.plusYears(edad.toLong())) edad--
    edad
} catch (e: Exception) { null }

fun String.toMillisOrNull(): Long? = try {
    val localDate = org.threeten.bp.LocalDate.parse(this)
    localDate.atStartOfDay(org.threeten.bp.ZoneId.systemDefault()).toInstant().toEpochMilli()
} catch (e: Exception) { null }

@RequiresApi(Build.VERSION_CODES.O)
fun calcularFechaHoraCita(fecha: String, hora: String): LocalDateTime? {
    return try {
        val diasEsEn = mapOf(
            "lunes" to "Monday", "martes" to "Tuesday", "miércoles" to "Wednesday", "jueves" to "Thursday",
            "viernes" to "Friday", "sábado" to "Saturday", "domingo" to "Sunday"
        )
        val mesesEsEn = mapOf(
            "enero" to "January", "febrero" to "February", "marzo" to "March", "abril" to "April",
            "mayo" to "May", "junio" to "June", "julio" to "July", "agosto" to "August",
            "septiembre" to "September", "octubre" to "October", "noviembre" to "November", "diciembre" to "December"
        )
        var input = "$fecha $hora"
            .replace("a.m.", "AM", ignoreCase = true)
            .replace("p.m.", "PM", ignoreCase = true)
            .replace('\u00A0', ' ')
            .replace(Regex("\\s+"), " ")
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
        val formato = DateTimeFormatter.ofPattern("EEEE, dd 'of' MMMM 'of' yyyy hh:mm a", Locale.ENGLISH)
        LocalDateTime.parse(input, formato)
    } catch (e: Exception) {
        null
    }
}
