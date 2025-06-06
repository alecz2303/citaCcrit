package com.alan.citascritapp.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.models.PacienteProfile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun extractTextFromPDF(context: Context, uri: Uri): String {
    context.contentResolver.openInputStream(uri).use { inputStream ->
        PDDocument.load(inputStream).use { document ->
            val pdfStripper = PDFTextStripper()
            return pdfStripper.getText(document)
        }
    }
    return ""
}

fun extraerCarnet(texto: String): String? {
    val carnetRegex = Regex("""Carnet\s+(\d{4,})""", RegexOption.IGNORE_CASE)
    return carnetRegex.find(texto)?.groupValues?.get(1)?.trim()
}

fun extraerCitas(texto: String): List<Cita> {
    val citas = mutableListOf<Cita>()
    val dias = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    val frasesPiePagina = listOf(
        "Debe acudir con 20 minutos de anticipación",
        "VISITA DOMICILIARIA",
        "Atención al Público para una cancelación",
        "Telemarketing",
        "cancelaciones@teleton-chp.org.mx",
        "Centro de Rehabilitación Infantil Teletón Página",
        "Citas del Paciente",
        "FECHA CITA SERVICIO MÉDICO CUBÍCULO"
    )
    val lineas = texto.lines().map { it.trim() }
    var i = 0

    while (i < lineas.size) {
        val l1 = lineas[i]

        if (frasesPiePagina.any { l1.contains(it, ignoreCase = true) }) {
            i++
            continue
        }

        if (dias.any { l1.lowercase().startsWith(it) }) {
            var medico = ""
            var cubiculo = ""
            var servicio = ""
            var hora = ""
            var fecha = ""

            // NUEVO REGEX: nombre del doctor con cubículo opcional al final
            val regexFecha = Regex(
                """^([a-záéíóúñ]+, \d{2} de [a-záéíóúñ]+ de \d{4}) (\d{2}:\d{2}\s*[ap]\.m\.) (.+?)(?: (\d+))?$""",
                RegexOption.IGNORE_CASE
            )
            val mFecha = regexFecha.find(l1)

            if (mFecha != null) {
                fecha = mFecha.groupValues[1]
                hora = mFecha.groupValues[2]
                medico = mFecha.groupValues[3].trim()
                cubiculo = mFecha.groupValues[4].trim()
                var j = i + 1

                // Acumular nombre del médico en líneas extra, si sigue sin número ni servicio ni basura
                while (
                    j < lineas.size &&
                    cubiculo.isBlank() && // Solo acumula si aún NO tenemos cubículo
                    !lineas[j].matches(Regex("""^\d+$""")) &&
                    !lineas[j].matches(Regex("""^[A-Z]{2,3} .+""")) &&
                    frasesPiePagina.none { lineas[j].contains(it, ignoreCase = true) } &&
                    !dias.any { lineas[j].lowercase().startsWith(it) }
                ) {
                    if (lineas[j].isNotBlank()) {
                        medico += " " + lineas[j]
                    }
                    j++
                }
                // Si cubículo sigue vacío, revisa si viene como número aislado
                if (cubiculo.isBlank() && j < lineas.size && lineas[j].matches(Regex("""^\d+$"""))) {
                    cubiculo = lineas[j]
                    j++
                }
                // Buscar el servicio (como ya tienes)
                var servicioEncontrado = false
                while (j < lineas.size) {
                    val linea = lineas[j]
                    if (dias.any { linea.lowercase().startsWith(it) }) break
                    if (frasesPiePagina.any { linea.contains(it, ignoreCase = true) }) {
                        j++
                        continue
                    }
                    val mServicio = Regex("""^[A-Z]{2,3} (.+?)(?: C ?(\d+))?$""").find(linea)
                    if (mServicio != null) {
                        servicio = mServicio.groupValues[1]
                        citas.add(Cita(fecha, hora, servicio, medico.trim(), cubiculo))
                        i = j + 1
                        servicioEncontrado = true
                        break
                    }
                    j++
                }
                if (servicioEncontrado) continue
            }
        }
        i++
    }
    return citas.distinctBy { "${it.fecha}${it.hora}${it.servicio}${it.medico}${it.cubiculo}" }
}

@RequiresApi(Build.VERSION_CODES.O)
fun procesarPDF(
    context: Context,
    uri: Uri,
    perfil: PacienteProfile?,
    onPerfilUpdate: (PacienteProfile) -> Unit,
    scope: CoroutineScope,
    onCitasCarnetReady: (List<Cita>, String?) -> Unit
) {
    val textoExtraido = extractTextFromPDF(context, uri)
    val nuevoCarnet = extraerCarnet(textoExtraido)
    val nuevasCitas = extraerCitas(textoExtraido)
    onCitasCarnetReady(nuevasCitas, nuevoCarnet)
    scope.launch {
        guardarCitas(context, nuevasCitas)
        guardarCarnet(context, nuevoCarnet)
        // Actualizar el perfil con el nuevo carnet si ya existe perfil
        if (perfil != null && nuevoCarnet != null) {
            val actualizado = perfil.copy(carnet = nuevoCarnet)
            onPerfilUpdate(actualizado)
        }
        // **Agregado: programa notificaciones**
        programarNotificacionesCitas(context, nuevasCitas)
    }
}
