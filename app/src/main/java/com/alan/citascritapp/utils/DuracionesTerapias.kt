package com.alan.citascritapp.utils

import java.text.Normalizer

object DuracionesTerapias {

    /** Mapa base con nombres exactos según PDF oficial */
    private val base = mapOf(
        // 🟣 Terapias 45 min
        "TF Terapia Física" to 45,
        "TF Terapia Física AD" to 45,
        "TF Ent. Robótico de la Marcha" to 45,
        "HI Tina Hubbard" to 45,
        "HI Tina Remolino" to 45,
        "TL Electroestimulación Oro Faríngea" to 45,
        "TP Terapia Pulmonar" to 45,
        "TP Terapia Pulmonar AD" to 45,
        "TF Baiobit" to 45,
        "TO Realidad Virtual" to 45,

        // 🟡 40 min
        "TO Terapia Ocupacional" to 40,
        "TO Terapia Ocupacional AD" to 40,
        "TL Terapia de Lenguaje" to 40,
        "TL Terapia de Lenguaje AD" to 40,
        "TO CIS" to 40,

        // 🟢 30 min
        "TF Terapia Física 30" to 30,
        "TF Terapia Física CEMS" to 30,
        "TF Realidad Virtual" to 30,
        "TO Terapia Ocupacional 30" to 30,
        "HI Tanque Terapéutico Grupal" to 30,
        "TL Terapia de Lenguaje 30" to 30,
        "TP Terapia Pulmonar 30" to 30,

        // 🔵 60 min
        "TO Terapia Ocupacional Grupal" to 60,
        "TL Terapia de Lenguaje Grupal" to 60,
        "TF Terapia Física Grupal" to 60,
        "TP Terapia Pulmonar Grupal" to 60,

        // 🟣 90 min
        "TO Terapia Ocupacional EDU" to 90,
        "TL Terapia de Lenguaje EDU" to 90,
        "TF Terapia Física EDU" to 90,

        // 🩺 Consultas 45
        "Valoración clínica" to 45,
        "Rehabilitación pulmonar" to 45,
        "Valoración social" to 45,

        // 🩵 Consultas 30
        "Genética" to 30,
        "Nutrición" to 30,
        "Neurología" to 30,
        "Pediatría" to 30,
        "Psicología familiar 30" to 30,
        "Enfermería EDU individual" to 30,

        // 🧩 Consultas 50
        "Asistencia tecnológica" to 50,
        "Entrevista apoyo pedagógico" to 50,
        "Apoyo pedagógico" to 50,
        "Psicología familiar" to 50,

        // 💚 Consultas 60
        "Nutrición EDU" to 60,
        "Pediatría EDU" to 60,
        "Rehabilitación pulmonar EDU" to 60,
        "Enfermería grupal EDU" to 60,

        // 💜 Grupos largos
        "Plática informativa TS" to 90,
        "Grupo de padres y madres" to 100,
        "Grupo de abuelos y abuelas" to 100,
        "Grupo de hermanos y hermanas" to 100,
        "Grupo de niños, niñas y adolescentes" to 100,
        "Apoyo pedagógico grupal" to 100
    )

    /**
     * Normaliza el texto eliminando tildes, mayúsculas y espacios extra
     */
    private fun normalizar(texto: String): String {
        return Normalizer.normalize(texto.lowercase().trim(), Normalizer.Form.NFD)
            .replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
    }

    /**
     * Obtiene la duración comparando texto completo, ignorando mayúsculas, tildes, etc.
     */
    fun obtenerDuracion(servicio: String): Int {
        val normalizado = normalizar(servicio)
        for ((nombre, minutos) in base) {
            if (normalizar(nombre) == normalizado) {
                return minutos
            }
        }
        return 30 // por defecto
    }
}
