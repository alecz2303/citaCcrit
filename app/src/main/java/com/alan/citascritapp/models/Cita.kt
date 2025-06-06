package com.alan.citascritapp.models

data class Cita(
    val fecha: String,
    val hora: String,
    val servicio: String,
    val medico: String,
    val cubiculo: String,
    val cancelada: Boolean = false
)