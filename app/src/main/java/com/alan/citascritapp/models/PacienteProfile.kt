package com.alan.citascritapp.models

data class PacienteProfile(
    val nombre: String,
    val apellidoP: String,
    val apellidoM: String,
    val fechaNacimiento: String,
    val fotoPath: String,
    val carnet: String
)

val perfilVacio = PacienteProfile("", "", "", "", "", "")
