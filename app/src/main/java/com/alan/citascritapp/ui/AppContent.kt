package com.alan.citascritapp.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.alan.citascritapp.utils.*
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.models.PacienteProfile
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import com.alan.citascritapp.utils.programarAlarmaCita
import com.alan.citascritapp.utils.cancelarTodasAlarmasCitas
import com.alan.citascritapp.utils.cancelarAlarmaCita // ¡Asegúrate de importar esto!
import android.provider.Settings

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    context: Context,
    perfil: PacienteProfile?,
    onEditPerfil: () -> Unit,
    onPerfilUpdate: (PacienteProfile) -> Unit,
    showBanner: (String, Color, ImageVector) -> Unit
) {
    var citas by remember { mutableStateOf<List<Cita>>(emptyList()) }
    var carnet by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var mostrarFinalizadas by remember { mutableStateOf(false) }

    // Estados para diálogo de cancelar cita
    var mostrarConfirmacionCancelar by remember { mutableStateOf(false) }
    var citaPendientePorCancelar by remember { mutableStateOf<Cita?>(null) }

    // AlertDialog state para PDF
    var showDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para mostrar el diálogo de permiso
    var mostrarDialogoPermisoAlarma by remember { mutableStateOf(false) }

    suspend fun recargarDatos() {
        citas = cargarCitas(context)
        carnet = cargarCarnet(context)
        Log.d("ALARMA_DEBUG", "Recargando datos, citas: ${citas.size}")
        citas.forEach { cita ->
            Log.d("ALARMA_DEBUG", "Evaluando cita ${cita.servicio} ${cita.fecha} ${cita.hora}")
            if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                Log.d("ALARMA_DEBUG", "Voy a programar la alarma de esta cita")
                programarAlarmaCita(
                    context,
                    cita,
                    onPermisoFaltante = { mostrarDialogoPermisoAlarma = true }
                )
            }
        }
        programarAlertasCitasDiaSiguiente(context, citas)
    }

    LaunchedEffect(Unit) {
        recargarDatos()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val isPdf = uri.toString().endsWith(".pdf", ignoreCase = true) ||
                    context.contentResolver.getType(uri) == "application/pdf"
            if (!isPdf) {
                showBanner(
                    "El archivo seleccionado no es un PDF.",
                    Color(0xFFD32F2F),
                    Icons.Default.Error
                )
                return@rememberLauncherForActivityResult
            }
            if (perfil == null ||
                perfil.nombre.isBlank() ||
                perfil.fechaNacimiento.isBlank() ||
                perfil.carnet.isBlank()
            ) {
                showBanner(
                    "Completa primero tu perfil antes de subir el PDF.",
                    Color(0xFFF9A825),
                    Icons.Default.Person
                )
                return@rememberLauncherForActivityResult
            }
            val textoExtraido = extractTextFromPDF(context, uri)
            val nuevoCarnet = extraerCarnet(textoExtraido)
            if (nuevoCarnet == null) {
                showBanner(
                    "El archivo no tiene formato válido del CRIT.",
                    Color(0xFFD32F2F),
                    Icons.Default.Warning
                )
                return@rememberLauncherForActivityResult
            }
            if (nuevoCarnet != perfil.carnet) {
                showBanner(
                    "El carnet de la agenda no coincide con el perfil.",
                    Color(0xFFD32F2F),
                    Icons.Default.Warning
                )
                return@rememberLauncherForActivityResult
            }
            if (citas.isNotEmpty()) {
                pendingUri = uri
                showDialog = true
            } else {
                scope.launch {
                    cancelarTodasAlarmasCitas(context)
                }
                procesarPDF(context, uri, perfil, onPerfilUpdate, scope) { nuevasCitas, nuevoCarnetStr ->
                    citas = nuevasCitas
                    carnet = nuevoCarnetStr
                    showBanner(
                        "Agenda cargada y citas actualizadas correctamente.",
                        Color(0xFF388E3C),
                        Icons.Default.CheckCircle
                    )
                    nuevasCitas.forEach { cita ->
                        if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                            programarAlarmaCita(
                                context,
                                cita,
                                onPermisoFaltante = { mostrarDialogoPermisoAlarma = true }
                            )
                        }
                    }
                    programarAlertasCitasDiaSiguiente(context, nuevasCitas)
                }
            }
        }
    }

    // ALERT DIALOG PARA CONFIRMAR BORRADO
    if (showDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false; pendingUri = null },
            title = { Text("¿Sobrescribir citas?") },
            text = { Text("Al subir una nueva agenda se reemplazarán todas las citas y carnet guardados previamente. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    pendingUri?.let { uri ->
                        scope.launch {
                            cancelarTodasAlarmasCitas(context)
                        }
                        procesarPDF(context, uri, perfil, onPerfilUpdate, scope) { nuevasCitas, nuevoCarnetStr ->
                            citas = nuevasCitas
                            carnet = nuevoCarnetStr
                            showBanner(
                                "Agenda cargada y citas actualizadas correctamente.",
                                Color(0xFF388E3C),
                                Icons.Default.CheckCircle
                            )
                            nuevasCitas.forEach { cita ->
                                if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                                    programarAlarmaCita(context, cita)
                                }
                            }
                            programarAlertasCitasDiaSiguiente(context, nuevasCitas)
                        }
                        pendingUri = null
                    }
                }) { Text("Sí, continuar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    pendingUri = null
                }) { Text("Cancelar") }
            }
        )
    }

    // ALERT DIALOG PARA CONFIRMAR CANCELACIÓN DE CITA
    if (mostrarConfirmacionCancelar && citaPendientePorCancelar != null) {
        AlertDialog(
            onDismissRequest = {
                mostrarConfirmacionCancelar = false
                citaPendientePorCancelar = null
            },
            title = { Text("¿Cancelar cita?") },
            text = { Text("¿Estás seguro de cancelar la cita de ${citaPendientePorCancelar?.servicio} a las ${citaPendientePorCancelar?.hora}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    val citaParaCancelar = citaPendientePorCancelar
                    if (citaParaCancelar != null) {
                        scope.launch {
                            cancelarAlarmaCita(context, citaParaCancelar)
                            citas = citas.map {
                                if (it == citaParaCancelar) it.copy(cancelada = true) else it
                            }
                            guardarCitas(context, citas)
                            Toast.makeText(
                                context,
                                "Cita cancelada: ${citaParaCancelar.servicio} ${citaParaCancelar.hora}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    mostrarConfirmacionCancelar = false
                    citaPendientePorCancelar = null
                }) {
                    Text("Sí, cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    mostrarConfirmacionCancelar = false
                    citaPendientePorCancelar = null
                }) {
                    Text("No")
                }
            }
        )
    }

    if (mostrarDialogoPermisoAlarma) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPermisoAlarma = false },
            title = { Text("Permiso necesario para alarmas exactas") },
            text = {
                Text(
                    "Para que la app pueda avisarte exactamente a la hora de tus citas, necesitas otorgar el permiso de 'Alarmas exactas'.\n\n" +
                            "Si no das este permiso, tus notificaciones podrían llegar tarde, no sonar, o incluso no aparecer.\n\n" +
                            "Por ejemplo: si tienes una cita importante y no diste este permiso, tu teléfono podría no avisarte a tiempo y corres el riesgo de olvidar tu cita.\n\n" +
                            "¿Quieres ir a la configuración para dar este permiso ahora?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoPermisoAlarma = false
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }) {
                    Text("Ir a configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPermisoAlarma = false }) {
                    Text("No, gracias")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Citas CRIT") },
                actions = {
                    TextButton(onClick = onEditPerfil) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Perfil",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            PerfilResumen(perfil = perfil)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = "Cargar agenda", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Cargar nueva agenda PDF", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "Sube aquí tu agenda de citas en formato PDF.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp, bottom = 12.dp)
            )

            // Switch para mostrar/ocultar finalizadas/canceladas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 6.dp)
            ) {
                Switch(
                    checked = mostrarFinalizadas,
                    onCheckedChange = { mostrarFinalizadas = it }
                )
                Spacer(Modifier.width(8.dp))
                Text("Mostrar citas finalizadas/canceladas")
            }

            // --- SwipeRefresh ENCIERRA el listado y contador ---
            val refreshScope = rememberCoroutineScope()
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = {
                    isRefreshing = true
                    refreshScope.launch {
                        recargarDatos()
                        isRefreshing = false
                        showBanner(
                            "Lista de citas actualizada.",
                            Color(0xFF1976D2),
                            Icons.Default.Refresh
                        )
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Muestra todas si está activo el switch, si no sólo activas
                    val citasFiltradas = if (mostrarFinalizadas) {
                        citas
                    } else {
                        citas.filter { !citaYaPaso(it.fecha, it.hora) && !it.cancelada }
                    }
                    val citasPorFecha = citasFiltradas.groupBy { it.fecha }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        citasPorFecha.forEach { (fecha, citasDeEseDia) ->
                            item {
                                Surface(
                                    color = Color(0xFFF6F6F6),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = fecha,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                                    )
                                }
                            }
                            items(citasDeEseDia, key = { it.hashCode() }) { cita ->
                                val (color, icon) = getCardColorAndIcon(cita.servicio)
                                val dismissState = rememberSwipeToDismissBoxState(
                                    positionalThreshold = { 60f },
                                    confirmValueChange = { value ->
                                        if (
                                            (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) &&
                                            !citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada
                                        ) {
                                            // Abre el diálogo, NO cancela aún
                                            mostrarConfirmacionCancelar = true
                                            citaPendientePorCancelar = cita
                                            // Cancelar swipe automático (volver a estado default)
                                            false
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromEndToStart = true,
                                    enableDismissFromStartToEnd = true,
                                    backgroundContent = {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(Color(0xFFF44336)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .fillMaxWidth()
                                                    .padding(end = 24.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    "CANCELAR",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.End,
                                                    modifier = Modifier.padding(end = 10.dp)
                                                )
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Cancelar cita",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = color),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                    ) {
                                        Box {
                                            Row(
                                                Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = cita.hora,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        when {
                                                            cita.cancelada -> {
                                                                Spacer(Modifier.width(6.dp))
                                                                AssistChip(
                                                                    onClick = {},
                                                                    label = { Text("Cancelada") },
                                                                    leadingIcon = {
                                                                        Icon(Icons.Default.Cancel, contentDescription = null)
                                                                    },
                                                                    colors = AssistChipDefaults.assistChipColors(
                                                                        containerColor = Color(0xFFB0BEC5),
                                                                        labelColor = Color(0xFF37474F),
                                                                        leadingIconContentColor = Color(0xFF37474F)
                                                                    )
                                                                )
                                                            }
                                                            citaYaPaso(cita.fecha, cita.hora) -> {
                                                                Spacer(Modifier.width(6.dp))
                                                                AssistChip(
                                                                    onClick = {},
                                                                    label = { Text("Finalizada") },
                                                                    leadingIcon = {
                                                                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                                                                    },
                                                                    colors = AssistChipDefaults.assistChipColors(
                                                                        containerColor = Color(0xFFFFCDD2),
                                                                        labelColor = Color(0xFFD32F2F),
                                                                        leadingIconContentColor = Color(0xFFD32F2F)
                                                                    )
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = cita.servicio,
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    Row(
                                                        Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = cita.medico,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                        Text(
                                                            text = "Cubículo: ${cita.cubiculo}",
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                }
                                            }
                                            if (citaYaPaso(cita.fecha, cita.hora)) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Finalizada",
                                                    tint = Color(0xFFF44336).copy(alpha = 0.75f),
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Contador fijo abajo
                    Text(
                        "Total de citas encontradas: ${citasFiltradas.size}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}
