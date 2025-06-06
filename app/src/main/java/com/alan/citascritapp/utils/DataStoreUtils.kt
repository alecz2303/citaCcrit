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
        "psicología" in s        -> Pair(Color(0xFFE3F2FD), Icons.Default.Psychology)
        "física" in s            -> Pair(Color(0xFFFFF9C4), Icons.Default.FitnessCenter)
        "ocupacional" in s       -> Pair(Color(0xFFE8F5E9), Icons.Default.Handyman)
        "tanque" in s || "hidro" in s -> Pair(Color(0xFFE1F5FE), Icons.Default.Pool)
        "social" in s            -> Pair(Color(0xFFFFE0B2), Icons.Default.People)
        "genética" in s          -> Pair(Color(0xFFF3E5F5), Icons.Default.Science)
        "valoración clínica" in s -> Pair(Color(0xFFB2DFDB), Icons.Default.MedicalServices)
        "asistencia tecnológica" in s -> Pair(Color(0xFFEEEEEE), Icons.Default.Memory)
        "neuropediatría" in s    -> Pair(Color(0xFFE1BEE7), Icons.Default.Psychology)
        "robótico" in s || "robotico" in s -> Pair(Color(0xFFB3E5FC), Icons.Default.SmartToy)
        "pediatría" in s || "pediatria" in s -> Pair(Color(0xFFFFF3E0), Icons.Default.ChildCare)
        else                     -> Pair(Color(0xFFFFFFFF), Icons.Default.EventNote)
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
