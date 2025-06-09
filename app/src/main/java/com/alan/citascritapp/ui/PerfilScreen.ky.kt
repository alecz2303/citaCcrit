package com.alan.citascritapp.ui

import android.net.Uri
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import com.alan.citascritapp.models.PacienteProfile
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.alan.citascritapp.utils.guardarImagenInterna
import com.alan.citascritapp.utils.toEdad
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import java.io.File
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.alan.citascritapp.utils.toLocalDateOrToday
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import java.time.format.DateTimeFormatter
import androidx.compose.material3.Snackbar
import androidx.compose.ui.text.input.TextFieldValue

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    context: Context,
    perfil: PacienteProfile?,
    onPerfilChange: (PacienteProfile) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    ocultarCancelar: Boolean = false
) {
    var nombre by remember { mutableStateOf(perfil?.nombre ?: "") }
    var apellidoP by remember { mutableStateOf(perfil?.apellidoP ?: "") }
    var apellidoM by remember { mutableStateOf(perfil?.apellidoM ?: "") }
    //var fechaNacimiento by remember { mutableStateOf(perfil?.fechaNacimiento ?: "") }
    var fotoPath by remember { mutableStateOf(perfil?.fotoPath ?: "") }
    var carnet by remember { mutableStateOf(perfil?.carnet ?: "") }
    var showSavedMessage by remember { mutableStateOf(false) }
    var imageRefreshKey by remember { mutableStateOf(0) }
    var showCarnetError by remember { mutableStateOf(false) }
    var showFechaError by remember { mutableStateOf(false) }

    // Date dialog state
    val dateDialogState = rememberMaterialDialogState()

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val path = guardarImagenInterna(context, uri)
            if (path != null) {
                fotoPath = path
                imageRefreshKey++
            }
        }
    }

    // Estado para el campo de fecha con manejo de cursor
    var fechaNacimientoState by remember { mutableStateOf(TextFieldValue(perfil?.fechaNacimiento ?: "")) }
    val fechaNacimiento = fechaNacimientoState.text

    val edad = fechaNacimiento.toEdad()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text(
            text = "Datos del Paciente",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .align(Alignment.Start)
        )

        // Imagen de perfil
        Box(
            modifier = Modifier
                .size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            val shape = CircleShape
            key(imageRefreshKey, fotoPath) {
                if (fotoPath.isNotBlank() && File(fotoPath).exists()) {
                    val bitmap = BitmapFactory.decodeFile(fotoPath)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Foto perfil",
                            modifier = Modifier
                                .size(110.dp)
                                .clip(shape)
                                .border(4.dp, MaterialTheme.colorScheme.primary, shape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Agregar foto",
                            modifier = Modifier
                                .size(110.dp)
                                .clip(shape)
                                .border(4.dp, MaterialTheme.colorScheme.primary, shape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Agregar foto",
                        modifier = Modifier
                            .size(110.dp)
                            .clip(shape)
                            .border(4.dp, MaterialTheme.colorScheme.primary, shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        // Botón Cambiar foto
        Button(
            onClick = { imageLauncher.launch("image/*") },
            modifier = Modifier
                .padding(top = 10.dp, bottom = 16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cambiar foto")
        }

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre(s)") },
            placeholder = { Text("Ej. Juan Miguel") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = apellidoP,
            onValueChange = { apellidoP = it },
            label = { Text("Apellido Paterno") },
            placeholder = { Text("Ej. Gómez") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = apellidoM,
            onValueChange = { apellidoM = it },
            label = { Text("Apellido Materno") },
            placeholder = { Text("Ej. Pérez") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = fechaNacimientoState,
            onValueChange = { value ->
                // Solo números y máximo 8 dígitos (AAAA MM DD)
                val limpio = value.text.filter { it.isDigit() }.take(8)
                val sb = StringBuilder()
                for (i in limpio.indices) {
                    sb.append(limpio[i])
                    if ((i == 3 || i == 5) && i != limpio.lastIndex) sb.append("-")
                }
                // Mantén el cursor al final
                val finalText = sb.toString()
                fechaNacimientoState = TextFieldValue(finalText, selection = androidx.compose.ui.text.TextRange(finalText.length))
            },
            label = { Text("Fecha de nacimiento (AAAA-MM-DD)") },
            placeholder = { Text("Ej. 2017-02-13") },
            //trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { dateDialogState.show() }
        )
        if (showFechaError && fechaNacimiento.isNotBlank()) {
            Text(
                text = "El formato debe ser AAAA-MM-DD (ejemplo: 2016-10-28)",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Edad: ${edad ?: "--"} años",
            style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 2.dp)
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = carnet,
            onValueChange = {
                carnet = it
                showCarnetError = false // oculta el error al escribir
            },
            label = { Text("Carnet") },
            placeholder = { Text("Obligatorio") },
            singleLine = true,
            isError = showCarnetError && carnet.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
        if (showCarnetError && carnet.isBlank()) {
            Text(
                text = "El campo Carnet es obligatorio para continuar.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    if (carnet.isBlank()) {
                        showCarnetError = true
                    } else if (fechaNacimiento.isBlank() || !fechaNacimiento.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                        showFechaError = true
                    } else {
                        showCarnetError = false
                        showFechaError = false
                        val nuevoPerfil = PacienteProfile(nombre, apellidoP, apellidoM, fechaNacimiento, fotoPath, carnet)
                        onPerfilChange(nuevoPerfil)
                        onGuardar()
                        showSavedMessage = true
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = nombre.isNotBlank() && apellidoP.isNotBlank() && apellidoM.isNotBlank() && fechaNacimiento.isNotBlank()
            ) { Text("Guardar") }
            OutlinedButton(
                onClick = {
                    nombre = perfil?.nombre ?: ""
                    apellidoP = perfil?.apellidoP ?: ""
                    apellidoM = perfil?.apellidoM ?: ""
                    fechaNacimientoState = (perfil?.fechaNacimiento ?: "") as TextFieldValue
                    fotoPath = perfil?.fotoPath ?: ""
                    carnet = perfil?.carnet ?: ""
                    imageRefreshKey++
                    onCancelar()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Cancelar") }
        }
        if (showSavedMessage) {
            Snackbar(
                action = {
                    TextButton(onClick = { showSavedMessage = false }) { Text("Cerrar") }
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Perfil guardado correctamente")
            }
        }
    }
}
