package com.alan.citascritapp.ui

import android.content.Context
import android.net.Uri
import android.os.Build
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

// ¡IMPORTANTE! Asegúrate que tu data class tenga cancelada: Boolean
// data class Cita(..., val cancelada: Boolean = false)

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

    // AlertDialog state
    var showDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    suspend fun recargarDatos() {
        citas = cargarCitas(context)
        carnet = cargarCarnet(context)
        citas.forEach { cita ->
            if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                programarNotificacionCita(context, cita)
            }
        }
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
                cancelarTodasNotificacionesCitas(context)
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
                            programarNotificacionCita(context, cita)
                        }
                    }
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
                        cancelarTodasNotificacionesCitas(context)
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
                                    programarNotificacionCita(context, cita)
                                }
                            }
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

            // Switch para mostrar/ocultar finalizadas
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
                                    confirmValueChange = { value ->
                                        if (
                                            value == SwipeToDismissBoxValue.EndToStart ||
                                            value == SwipeToDismissBoxValue.StartToEnd
                                        ) {
                                            // Solo permite cancelar si NO está finalizada/cancelada
                                            if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                                                scope.launch {
                                                    // Marca como cancelada
                                                    citas = citas.map {
                                                        if (it == cita) it.copy(cancelada = true) else it
                                                    }
                                                    guardarCitas(context, citas)
                                                }
                                            }
                                            true
                                        } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFFF44336)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Cancelar cita",
                                                tint = Color.White,
                                                modifier = Modifier.padding(end = 24.dp)
                                            )
                                        }
                                    },
                                    enableDismissFromEndToStart = true,
                                    enableDismissFromStartToEnd = true,
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
