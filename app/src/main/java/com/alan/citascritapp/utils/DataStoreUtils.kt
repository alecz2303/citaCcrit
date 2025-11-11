package com.alan.citascritapp.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*
import androidx.annotation.RequiresApi
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.models.PacienteProfile
import com.alan.citascritapp.utils.DuracionesTerapias


val Context.dataStore by preferencesDataStore(name = "citas_datastore")
val CITAS_KEY = stringPreferencesKey("citas_json")
val CARNET_KEY = stringPreferencesKey("carnet")
val PERFIL_KEY = stringPreferencesKey("perfil_json")

// --- Saber si la cita ya pasó ---
fun citaYaPaso(fecha: String, hora: String): Boolean {
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

    val formato = SimpleDateFormat("EEEE, dd 'of' MMMM 'of' yyyy hh:mm a", Locale.ENGLISH)
    return try {
        val fechaHora = formato.parse(input)
        fechaHora != null && fechaHora.before(Date())
    } catch (e: Exception) {
        false
    }
}

// --- Colores e íconos por servicio ---
fun getCardColorAndIcon(servicio: String): Pair<Color, ImageVector> {
    val s = servicio.lowercase()
    return when {
        // --- TERAPIAS PRINCIPALES ---
        "psicología" in s -> Pair(Color(0xFFE3F2FD), Icons.Default.Psychology)
        "física" in s -> Pair(Color(0xFFFFF9C4), Icons.Default.FitnessCenter)
        "ocupacional" in s -> Pair(Color(0xFFE8F5E9), Icons.Default.Handyman)
        "lenguaje" in s || "comunicación" in s -> Pair(Color(0xFFFFECB3), Icons.Default.RecordVoiceOver)
        "tanque" in s || "hidro" in s || "acuática" in s -> Pair(Color(0xFFE1F5FE), Icons.Default.Pool)

        // --- ÁREAS MÉDICAS ---
        "pediatría" in s || "pediatria" in s -> Pair(Color(0xFFFFF3E0), Icons.Default.ChildCare)
        "neuropediatría" in s || "neuropediatria" in s -> Pair(Color(0xFFE1BEE7), Icons.Default.Psychology)
        "genética" in s -> Pair(Color(0xFFF3E5F5), Icons.Default.Science)
        "nutrición" in s || "nutricion" in s -> Pair(Color(0xFFE8F5E9), Icons.Default.Restaurant)
        "odontología" in s || "odontologia" in s -> Pair(Color(0xFFFFEBEE), Icons.Default.MedicalServices)
        "oftalmología" in s || "oftalmologia" in s -> Pair(Color(0xFFE0F7FA), Icons.Default.RemoveRedEye)
        "otorrino" in s || "otorrinolaringología" in s || "otorrinolaringologia" in s ->
            Pair(Color(0xFFF3E5F5), Icons.Default.Hearing)
        "ortopedia" in s -> Pair(Color(0xFFEDE7F6), Icons.Default.HealthAndSafety)
        "medicina física" in s || "rehabilitación" in s || "rehabilitacion" in s ->
            Pair(Color(0xFFE0F2F1), Icons.Default.LocalHospital)
        "valoración clínica" in s || "valoracion clínica" in s || "valoracion clinica" in s ->
            Pair(Color(0xFFB2DFDB), Icons.Default.MedicalServices)

        // --- OTRAS ÁREAS DEL CRIT ---
        "trabajo social" in s || "social" in s -> Pair(Color(0xFFFFE0B2), Icons.Default.People)
        "asistencia tecnológica" in s || "asistencia tecnologica" in s -> Pair(Color(0xFFEEEEEE), Icons.Default.Memory)
        "robótico" in s || "robotico" in s -> Pair(Color(0xFFB3E5FC), Icons.Default.SmartToy)
        "integración educativa" in s || "integracion educativa" in s ->
            Pair(Color(0xFFE8EAF6), Icons.Default.School)
        "taller de padres" in s -> Pair(Color(0xFFFFF8E1), Icons.Default.Groups)
        "vida independiente" in s -> Pair(Color(0xFFF1F8E9), Icons.Default.SelfImprovement)

        // --- DEFAULT / NO CLASIFICADO ---
        else -> Pair(Color(0xFFFFFFFF), Icons.Default.EventNote)
    }
}

// Extensión para String a LocalDate con fallback
@RequiresApi(Build.VERSION_CODES.O)
fun String.toLocalDateOrToday(): LocalDate {
    return try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        LocalDate.now()
    }
}

// --- Guardar la imagen internamente ---
fun guardarImagenInterna(context: Context, uri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val dir = File(context.filesDir, "profile")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "profile.jpg")
            val outStream = FileOutputStream(file)
            inputStream.copyTo(outStream)
            inputStream.close()
            outStream.close()
            file.absolutePath
        } else null
    } catch (e: Exception) {
        null
    }
}

fun citaEnCurso(fecha: String, hora: String, servicio: String): Boolean {
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
            .replace("a. m.", "AM", ignoreCase = true)
            .replace("p. m.", "PM", ignoreCase = true)
            .replace("a.m.", "AM", ignoreCase = true)
            .replace("p.m.", "PM", ignoreCase = true)
            .replace("\u00A0", " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 🔤 Traducir días/meses si vienen en español
        for ((es, en) in diasEsEn) input = input.replace(es, en, ignoreCase = true)
        for ((es, en) in mesesEsEn) input = input.replace(es, en, ignoreCase = true)

        // 🧹 Eliminar “de” duplicados para inglés
        input = input.replace(" de ", " ", ignoreCase = true)
        input = input.replace(" of ", " ", ignoreCase = true)

        val posiblesFormatos = listOf(
            "EEEE, d MMMM yyyy h:mm a",
            "EEEE d MMMM yyyy h:mm a",
            "EEEE, d MMMM yyyy hh:mm a",
            "EEEE d MMMM yyyy hh:mm a"
        )

        var start: java.time.LocalDateTime? = null
        for (formato in posiblesFormatos) {
            try {
                val formatter = java.time.format.DateTimeFormatter.ofPattern(formato, java.util.Locale.ENGLISH)
                start = java.time.LocalDateTime.parse(input, formatter)
                break
            } catch (_: Exception) { }
        }

        if (start == null) {
            android.util.Log.w("CITA_EN_CURSO", "No se pudo parsear fecha/hora (limpia): $input")
            return false
        }

        val dur = DuracionesTerapias.obtenerDuracion(servicio)
        val end = start.plusMinutes(dur.toLong())

        val now = java.time.LocalDateTime.now()
        val enCurso = now.isAfter(start) && now.isBefore(end)

        android.util.Log.d("CITA_EN_CURSO", "Servicio=$servicio  Inicio=$start  Fin=$end  Ahora=$now  EnCurso=$enCurso")

        enCurso
    } catch (e: Exception) {
        android.util.Log.e("CITA_EN_CURSO", "Error al calcular: ${e.message}")
        false
    }
}

suspend fun guardarCitas(context: Context, citas: List<Cita>) {
    val gson = Gson()
    val citasJson = gson.toJson(citas)
    context.dataStore.edit { prefs ->
        prefs[CITAS_KEY] = citasJson
    }
}

suspend fun cargarCitas(context: Context): List<Cita> {
    val prefs = context.dataStore.data.first()
    val citasJson = prefs[CITAS_KEY] ?: return emptyList()
    val gson = Gson()
    val type = object : TypeToken<List<Cita>>() {}.type
    return gson.fromJson(citasJson, type)
}

suspend fun guardarCarnet(context: Context, carnet: String?) {
    context.dataStore.edit { prefs ->
        if (carnet != null) prefs[CARNET_KEY] = carnet else prefs.remove(CARNET_KEY)
    }
}

suspend fun cargarCarnet(context: Context): String? {
    val prefs = context.dataStore.data.first()
    return prefs[CARNET_KEY]
}

suspend fun guardarPerfil(context: Context, perfil: PacienteProfile) {
    val gson = Gson()
    val perfilJson = gson.toJson(perfil)
    context.dataStore.edit { prefs -> prefs[PERFIL_KEY] = perfilJson }
}

suspend fun cargarPerfil(context: Context): PacienteProfile? {
    val prefs = context.dataStore.data.first()
    val perfilJson = prefs[PERFIL_KEY] ?: return null
    val gson = Gson()
    return gson.fromJson(perfilJson, PacienteProfile::class.java)
}
