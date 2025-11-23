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
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import java.io.File
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.alan.citascritapp.ui.theme.CritBlue
import com.alan.citascritapp.ui.theme.CritPurple
import com.alan.citascritapp.utils.toLocalDateOrToday
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import java.time.format.DateTimeFormatter

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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // --- HEADER HERO ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // Fondo Gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(CritBlue, CritPurple)
                            )
                        )
                )

                // Título
                Text(
                    text = "Editar Perfil",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp)
                )

                // Foto de Perfil Superpuesta
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(120.dp)
                ) {
                    val shape = CircleShape
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(shape)
                            .border(4.dp, MaterialTheme.colorScheme.background, shape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        key(imageRefreshKey, fotoPath) {
                            if (fotoPath.isNotBlank() && File(fotoPath).exists()) {
                                val bitmap = BitmapFactory.decodeFile(fotoPath)
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Foto perfil",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp),
                                        tint = Color.Gray
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }

                    // Botón de Cámara Flotante
                    SmallFloatingActionButton(
                        onClick = { imageLauncher.launch("image/*") },
                        modifier = Modifier.align(Alignment.BottomEnd),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Cambiar foto", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // --- FORMULARIO ---
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Datos Personales
                Text(
                    "Información Personal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre(s)") },
                    placeholder = { Text("Ej. Juan Miguel") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = apellidoP,
                    onValueChange = { apellidoP = it },
                    label = { Text("Apellido Paterno") },
                    placeholder = { Text("Ej. Gómez") },
                    leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = apellidoM,
                    onValueChange = { apellidoM = it },
                    label = { Text("Apellido Materno") },
                    placeholder = { Text("Ej. Pérez") },
                    leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Fecha de Nacimiento
                OutlinedTextField(
                    value = fechaNacimientoState,
                    onValueChange = { value ->
                        val limpio = value.text.filter { it.isDigit() }.take(8)
                        val sb = StringBuilder()
                        for (i in limpio.indices) {
                            sb.append(limpio[i])
                            if ((i == 3 || i == 5) && i != limpio.lastIndex) sb.append("-")
                        }
                        val finalText = sb.toString()
                        fechaNacimientoState = TextFieldValue(finalText, selection = androidx.compose.ui.text.TextRange(finalText.length))
                    },
                    label = { Text("Fecha de nacimiento") },
                    placeholder = { Text("AAAA-MM-DD") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { dateDialogState.show() }) {
                            Icon(Icons.Default.EditCalendar, contentDescription = "Seleccionar fecha")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dateDialogState.show() },
                    shape = RoundedCornerShape(12.dp),
                    enabled = true // Permitir edición manual también
                )
                
                if (showFechaError && fechaNacimiento.isNotBlank()) {
                    Text(
                        text = "Formato inválido. Usa AAAA-MM-DD",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                if (edad != null) {
                    Text(
                        text = "Edad: $edad años",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))

                // Datos Médicos
                Text(
                    "Datos Médicos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = carnet,
                    onValueChange = {
                        carnet = it
                        showCarnetError = false
                    },
                    label = { Text("Número de Carnet") },
                    placeholder = { Text("Obligatorio") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    isError = showCarnetError && carnet.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                if (showCarnetError && carnet.isBlank()) {
                    Text(
                        text = "El carnet es obligatorio.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Botones de Acción
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!ocultarCancelar) {
                        OutlinedButton(
                            onClick = {
                                // Restaurar valores
                                nombre = perfil?.nombre ?: ""
                                apellidoP = perfil?.apellidoP ?: ""
                                apellidoM = perfil?.apellidoM ?: ""
                                fechaNacimientoState = TextFieldValue(perfil?.fechaNacimiento ?: "")
                                fotoPath = perfil?.fotoPath ?: ""
                                carnet = perfil?.carnet ?: ""
                                imageRefreshKey++
                                onCancelar()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }
                    }

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
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = nombre.isNotBlank() && apellidoP.isNotBlank() && apellidoM.isNotBlank() && fechaNacimiento.isNotBlank()
                    ) {
                        Text("Guardar")
                    }
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Date Picker Dialog
    MaterialDialog(
        dialogState = dateDialogState,
        buttons = {
            positiveButton(text = "Aceptar")
            negativeButton(text = "Cancelar")
        }
    ) {
        datepicker(
            initialDate = fechaNacimiento.toLocalDateOrToday(),
            title = "Fecha de nacimiento"
        ) { date ->
            val formattedDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            fechaNacimientoState = TextFieldValue(formattedDate, selection = androidx.compose.ui.text.TextRange(formattedDate.length))
        }
    }

    if (showSavedMessage) {
        Snackbar(
            action = {
                TextButton(onClick = { showSavedMessage = false }) { Text("Cerrar") }
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Perfil guardado correctamente")
        }
    }
}
