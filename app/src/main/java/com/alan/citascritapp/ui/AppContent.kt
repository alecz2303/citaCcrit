package com.alan.citascritapp.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.alan.citascritapp.R
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.models.PacienteProfile
import com.alan.citascritapp.utils.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Pantalla principal de Citas CRIT (Compose)
 * - Mantiene flujo original: perfil, carga PDF, banners, TTS, alarmas.
 * - Mejora visual: chips de estado, swipe naranja, duración visible, top bar estilizada.
 */
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
    // -------------------------
    // ESTADOS PRINCIPALES
    // -------------------------
    var citas by remember { mutableStateOf<List<Cita>>(emptyList()) }
    var carnet by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var mostrarFinalizadas by remember { mutableStateOf(false) }

    // Estados de diálogo para "marcar como no se realizará"
    var mostrarConfirmacionCancelar by remember { mutableStateOf(false) }
    var citaPendientePorCancelar by remember { mutableStateOf<Cita?>(null) }

    // AlertDialog de confirmación para sobrescribir agenda
    var showDialogSobrescribir by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // Permiso de alarmas exactas
    var mostrarDialogoPermisoAlarma by remember { mutableStateOf(false) }

    // Estado para mostrar el diálogo con el dino (GIF + audio)
    var showDinoDialog by remember { mutableStateOf(false) }

    // --- Integración TTS (Texto a Voz) ---
    val ttsHelper = remember { TTSHelper(context) }
    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }

    // -------------------------
    // FUNCIÓN: recargar datos
    // -------------------------
    suspend fun recargarDatos() {
        citas = cargarCitas(context)
        carnet = cargarCarnet(context)
        Log.d("ALARMA_DEBUG", "Recargando datos, citas: ${citas.size}")
        citas.forEach { cita ->
            Log.d("ALARMA_DEBUG", "Evaluando cita ${cita.servicio} ${cita.fecha} ${cita.hora}")
            if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                // Programa alarmas por cada cita activa
                programarAlarmaCita(
                    context,
                    cita,
                    onPermisoFaltante = { mostrarDialogoPermisoAlarma = true }
                )
            }
        }
        // Programa alerta de "citas de mañana"
        programarAlertasCitasDiaSiguiente(context, citas)
    }

    // Carga inicial de datos
    LaunchedEffect(Unit) { recargarDatos() }

    // -------------------------
    // LAUNCHER: seleccionar PDF
    // -------------------------
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val isPdf = uri.toString().endsWith(".pdf", ignoreCase = true) ||
                    context.contentResolver.getType(uri) == "application/pdf"
            if (!isPdf) {
                showBanner("El archivo seleccionado no es un PDF.", Color(0xFFD32F2F), Icons.Default.Error)
                return@rememberLauncherForActivityResult
            }
            // Validación de perfil antes de procesar agenda
            if (perfil == null || perfil.nombre.isBlank() || perfil.fechaNacimiento.isBlank() || perfil.carnet.isBlank()) {
                showBanner("Completa primero tu perfil antes de subir el PDF.", Color(0xFFF9A825), Icons.Default.Person)
                return@rememberLauncherForActivityResult
            }
            val textoExtraido = extractTextFromPDF(context, uri)
            val nuevoCarnet = extraerCarnet(textoExtraido)
            if (nuevoCarnet == null) {
                showBanner("El archivo no tiene formato válido del CRIT.", Color(0xFFD32F2F), Icons.Default.Warning)
                return@rememberLauncherForActivityResult
            }
            if (nuevoCarnet != perfil.carnet) {
                showBanner("El carnet de la agenda no coincide con el perfil.", Color(0xFFD32F2F), Icons.Default.Warning)
                return@rememberLauncherForActivityResult
            }
            // Si ya había citas cargadas, pedimos confirmación para sobrescribir
            if (citas.isNotEmpty()) {
                pendingUri = uri
                showDialogSobrescribir = true
            } else {
                scope.launch { cancelarTodasAlarmasCitas(context) }
                procesarPDF(context, uri, perfil, onPerfilUpdate, scope) { nuevasCitas, nuevoCarnetStr ->
                    // Reemplaza citas y carnet
                    citas = nuevasCitas
                    carnet = nuevoCarnetStr
                    showBanner("Agenda cargada y citas actualizadas correctamente.", Color(0xFF388E3C), Icons.Default.CheckCircle)
                    // Programa alarmas por cada cita activa
                    nuevasCitas.forEach { cita ->
                        if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                            programarAlarmaCita(context, cita, onPermisoFaltante = { mostrarDialogoPermisoAlarma = true })
                        }
                    }
                    programarAlertasCitasDiaSiguiente(context, nuevasCitas)
                }
            }
        }
    }

    // -------------------------
    // DIALOG: sobrescribir agenda
    // -------------------------
    if (showDialogSobrescribir && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showDialogSobrescribir = false; pendingUri = null },
            title = { Text("¿Sobrescribir citas?") },
            text = { Text("Al subir una nueva agenda se reemplazarán todas las citas y carnet guardados previamente. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialogSobrescribir = false
                    pendingUri?.let { uri ->
                        scope.launch { cancelarTodasAlarmasCitas(context) }
                        procesarPDF(context, uri, perfil, onPerfilUpdate, scope) { nuevasCitas, nuevoCarnetStr ->
                            citas = nuevasCitas
                            carnet = nuevoCarnetStr
                            showBanner("Agenda cargada y citas actualizadas correctamente.", Color(0xFF388E3C), Icons.Default.CheckCircle)
                            nuevasCitas.forEach { cita ->
                                if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                                    programarAlarmaCita(context, cita, onPermisoFaltante = { mostrarDialogoPermisoAlarma = true })
                                }
                            }
                            programarAlertasCitasDiaSiguiente(context, nuevasCitas)
                        }
                        pendingUri = null
                    }
                }) { Text("Sí, continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogSobrescribir = false; pendingUri = null }) { Text("Cancelar") }
            }
        )
    }

    // -------------------------
    // DIALOG: permiso de alarmas exactas
    // -------------------------
    if (mostrarDialogoPermisoAlarma) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPermisoAlarma = false },
            title = { Text("Permiso necesario para alarmas exactas") },
            text = {
                Text(
                    "Para que la app pueda avisarte exactamente a la hora de tus citas, necesitas otorgar el permiso de 'Alarmas exactas'.\n\n" +
                            "Si no das este permiso, tus notificaciones podrían llegar tarde o incluso no aparecer.\n\n" +
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
                }) { Text("Ir a configuración") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPermisoAlarma = false }) { Text("No, gracias") }
            }
        )
    }

    // -------------------------
    // TOP APP BAR estilizada (azul)
    // -------------------------
    val azulClaro = Color(0xFFE3F2FD)   // fondo appbar
    val azulPrimario = Color(0xFF1976D2) // texto e íconos appbar

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Citas CRIT",
                            color = azulPrimario,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDinoDialog = true }, // ✅ restaurado
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                painterResource(id = R.drawable.ic_dino_notif),
                                contentDescription = "Dino",
                                modifier = Modifier.size(24.dp),
                                tint = azulPrimario
                            )
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onEditPerfil) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Perfil",
                            modifier = Modifier.size(26.dp),
                            tint = azulPrimario
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Perfil",
                            color = azulPrimario,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = azulClaro)
            )
        }
    ) { padding ->
        // Al tocar el dino del AppBar, abrimos el dialog con GIF + sonido
        LaunchedEffect(Unit) {
            // noop inicial
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Resumen de perfil (si existe)
            PerfilResumen(perfil = perfil, onEditPerfil = onEditPerfil)
            Spacer(Modifier.height(12.dp))

            // Botón principal: Cargar PDF (azul institucional)
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = azulPrimario,
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

            // Switch para mostrar/ocultar finalizadas / no realizadas
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
                Text("Mostrar citas finalizadas / Canceladas")
            }

            // SwipeRefresh: recarga visual
            val refreshScope = rememberCoroutineScope()
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = {
                    isRefreshing = true
                    refreshScope.launch {
                        recargarDatos()
                        isRefreshing = false
                        showBanner("Lista de citas actualizada.", Color(0xFF1976D2), Icons.Default.Refresh)
                    }
                }
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filtro: muestra todas si el switch está activo;
                    // si no, muestra futuras y EN CURSO, excluye canceladas.
                    val citasFiltradas = if (mostrarFinalizadas) {
                        citas
                    } else {
                        citas.filter {
                            (!citaYaPaso(it.fecha, it.hora) || citaEnCurso(it.fecha, it.hora, it.servicio)) && !it.cancelada
                        }
                    }

                    val citasPorFecha = citasFiltradas.groupBy { it.fecha }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        citasPorFecha.forEach { (fecha, citasDeEseDia) ->
                            // Encabezado por fecha
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

                                // --- Estado por cita ---
                                var abrirDialogo by remember { mutableStateOf(false) }
                                var citaSeleccionada by remember { mutableStateOf<Cita?>(null) }

                                // Control de vibración háptica
                                val haptic = LocalHapticFeedback.current

                                // 🔒 Solo permite swipe si la cita NO está cancelada ni finalizada
                                val puedeDeslizar = !cita.cancelada && !citaYaPaso(cita.fecha, cita.hora)

                                // 🧩 Estado del swipe (unico por cita)
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (puedeDeslizar &&
                                            (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart)
                                        ) {
                                            citaSeleccionada = cita
                                            abrirDialogo = true
                                            // 💥 Vibración corta para confirmación táctil
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            false // evita que se quede en naranja
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromEndToStart = puedeDeslizar,
                                    enableDismissFromStartToEnd = puedeDeslizar,
                                    backgroundContent = {
                                        if (puedeDeslizar) {
                                            Box(
                                                Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(22.dp))
                                                    .background(Color(0xFFFFE0B2)),
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
                                                        "MARCAR COMO CANCELADA",
                                                        color = Color(0xFFE65100),
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.End,
                                                        modifier = Modifier.padding(end = 10.dp)
                                                    )
                                                    Icon(
                                                        Icons.Default.EventBusy,
                                                        contentDescription = "Cancelada",
                                                        tint = Color(0xFFE65100),
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    content = {
                                        // 💡 Tu Card de cita va igual aquí
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val texto =
                                                        "Cita de ${cita.servicio}, el ${cita.fecha}, a las ${cita.hora}, con ${cita.medico}"
                                                    ttsHelper.speak(texto)
                                                },
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
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        // Hora y Chips de estado
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = cita.hora,
                                                                style = MaterialTheme.typography.titleMedium
                                                            )
                                                            when {
                                                                // 🟠 No se realizará
                                                                cita.cancelada -> {
                                                                    Spacer(Modifier.width(6.dp))
                                                                    AssistChip(
                                                                        onClick = {},
                                                                        label = {
                                                                            Text(
                                                                                "Cancelada",
                                                                                style = MaterialTheme.typography.titleSmall,
                                                                                fontWeight = FontWeight.Medium
                                                                            )
                                                                        },
                                                                        leadingIcon = { Icon(Icons.Default.EventBusy, contentDescription = null) },
                                                                        colors = AssistChipDefaults.assistChipColors(
                                                                            containerColor = Color(0xFFFFE0B2),
                                                                            labelColor = Color(0xFFE65100),
                                                                            leadingIconContentColor = Color(0xFFE65100)
                                                                        ),
                                                                        elevation = AssistChipDefaults.assistChipElevation(4.dp)
                                                                    )
                                                                }

                                                                // 🟢 En curso (con hora de fin)
                                                                citaEnCurso(cita.fecha, cita.hora, cita.servicio) -> {
                                                                    Spacer(Modifier.width(6.dp))

                                                                    val duracionEnMin = DuracionesTerapias.obtenerDuracion(cita.servicio)
                                                                    var horaFinTexto = ""

                                                                    try {
                                                                        // Normaliza formato de hora (AM / a.m. / etc.)
                                                                        val horaLimpia = cita.hora
                                                                            .replace("a. m.", "AM", true)
                                                                            .replace("p. m.", "PM", true)
                                                                            .replace("a.m.", "AM", true)
                                                                            .replace("p.m.", "PM", true)
                                                                            .trim()

                                                                        // Patrones probables
                                                                        val patrones = listOf("hh:mm a", "h:mm a", "hh:mm aa", "h:mm aa")
                                                                        var horaFin: Date? = null
                                                                        for (patron in patrones) {
                                                                            try {
                                                                                val parser = SimpleDateFormat(patron, Locale.ENGLISH)
                                                                                val inicio = parser.parse(horaLimpia)
                                                                                if (inicio != null) {
                                                                                    val fin = Calendar.getInstance().apply {
                                                                                        time = inicio
                                                                                        add(Calendar.MINUTE, duracionEnMin)
                                                                                    }.time
                                                                                    horaFin = fin
                                                                                    break
                                                                                }
                                                                            } catch (_: Exception) {}
                                                                        }

                                                                        if (horaFin != null) {
                                                                            val formatoSalida = SimpleDateFormat("h:mm a", Locale.getDefault())
                                                                            horaFinTexto = formatoSalida.format(horaFin)
                                                                        } else {
                                                                            Log.w("HORA_FIN", "No se pudo parsear hora: ${cita.hora}")
                                                                        }
                                                                    } catch (e: Exception) {
                                                                        Log.e("HORA_FIN", "Error al calcular fin: ${e.message}")
                                                                    }

                                                                    val textoChip = if (horaFinTexto.isNotEmpty()) {
                                                                        "En curso hasta las $horaFinTexto"
                                                                    } else "En curso"

                                                                    AssistChip(
                                                                        onClick = {},
                                                                        label = {
                                                                            Text(
                                                                                textoChip,
                                                                                style = MaterialTheme.typography.titleSmall
                                                                            )
                                                                        },
                                                                        leadingIcon = {
                                                                            Icon(
                                                                                Icons.Default.Circle,
                                                                                contentDescription = null,
                                                                                tint = Color(0xFF2E7D32),
                                                                                modifier = Modifier.size(10.dp)
                                                                            )
                                                                        },
                                                                        colors = AssistChipDefaults.assistChipColors(
                                                                            containerColor = Color(0xFFC8E6C9),
                                                                            labelColor = Color(0xFF2E7D32),
                                                                            leadingIconContentColor = Color(0xFF2E7D32)
                                                                        )
                                                                    )
                                                                }

                                                                // 🔴 Finalizada
                                                                citaYaPaso(cita.fecha, cita.hora) -> {
                                                                    Spacer(Modifier.width(6.dp))
                                                                    AssistChip(
                                                                        onClick = {},
                                                                        label = { Text("Finalizada") },
                                                                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                                                        colors = AssistChipDefaults.assistChipColors(
                                                                            containerColor = Color(0xFFFFCDD2),
                                                                            labelColor = Color(0xFFD32F2F),
                                                                            leadingIconContentColor = Color(0xFFD32F2F)
                                                                        )
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        // Servicio
                                                        Text(text = cita.servicio, style = MaterialTheme.typography.bodyLarge)

                                                        // 🕒 Duración visible
                                                        val duracion = DuracionesTerapias.obtenerDuracion(cita.servicio)
                                                        Text(
                                                            text = "Duración: ${duracion} min",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )

                                                        // Médico y Cubículo
                                                        Row(
                                                            Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(text = cita.medico, style = MaterialTheme.typography.bodySmall)
                                                            Text(text = "Cubículo: ${cita.cubiculo}", style = MaterialTheme.typography.bodySmall)
                                                        }

                                                        // ℹ️ Bloque informativo si no se realizará
                                                        if (cita.cancelada) {
                                                            Row(
                                                                Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(top = 4.dp)
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(Color(0xFFFFF3E0))
                                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Icon(
                                                                    Icons.Default.Info,
                                                                    contentDescription = "Aviso",
                                                                    tint = Color(0xFFE65100),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                                Spacer(Modifier.width(6.dp))
                                                                Text(
                                                                    "El CRIT informó que esta cita se Cancela.",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = Color(0xFFE65100)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                // Marca visual sutil en la esquina para finalizadas (opcional)
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
                                )

                                // --- Diálogo de confirmación ---
                                if (abrirDialogo && citaSeleccionada != null) {
                                    AlertDialog(
                                        onDismissRequest = {
                                            abrirDialogo = false
                                            citaSeleccionada = null
                                            scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                                        },
                                        title = { Text("Marcar como Cancelada") },
                                        text = {
                                            Text(
                                                "¿El CRIT te informó que la cita de ${citaSeleccionada?.servicio} a las ${citaSeleccionada?.hora} se Cancela?\n\n" +
                                                        "Esta acción solo actualizará el estado en la app y es de carácter informativo."
                                            )
                                        },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                scope.launch {
                                                    citaSeleccionada?.let { citaParaActualizar ->
                                                        cancelarAlarmaCita(context, citaParaActualizar)
                                                        citas = citas.map {
                                                            if (it == citaParaActualizar) it.copy(cancelada = true) else it
                                                        }
                                                        guardarCitas(context, citas)
                                                        Toast.makeText(
                                                            context,
                                                            "Marcada como no se realizará: ${citaParaActualizar.servicio} ${citaParaActualizar.hora}",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                    abrirDialogo = false
                                                    citaSeleccionada = null
                                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                                }
                                            }) { Text("Sí, marcar") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = {
                                                abrirDialogo = false
                                                citaSeleccionada = null
                                                scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                                            }) { Text("No") }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Contador
                    Text(
                        "Total de citas mostradas: ${citasFiltradas.size}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // -------------------------
    // DIALOG: Dino con GIF + sonido
    // -------------------------
    if (showDinoDialog) {
        val ctx = LocalContext.current

        // Reproduce sonido al mostrar el diálogo
        LaunchedEffect(key1 = showDinoDialog) {
            if (showDinoDialog) {
                val mediaPlayer = MediaPlayer.create(ctx, R.raw.marimba_ritmo)
                mediaPlayer.start()
                mediaPlayer.setOnCompletionListener { it.release() }
            }
        }

        AlertDialog(
            onDismissRequest = { showDinoDialog = false },
            confirmButton = {
                TextButton(onClick = { showDinoDialog = false }) { Text("Cerrar") }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("¡Bailecito del Nino Nino! 🦕")
                        Spacer(Modifier.height(10.dp))
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data("file:///android_asset/dino_dance.gif")
                                .decoderFactory(GifDecoder.Factory())
                                .build(),
                            contentDescription = "Dino bailando",
                            modifier = Modifier.size(250.dp)
                        )
                        Text("¡Con Cariño Alan Rodrigo! 💛💜")
                    }
                }
            }
        )
    }
}
