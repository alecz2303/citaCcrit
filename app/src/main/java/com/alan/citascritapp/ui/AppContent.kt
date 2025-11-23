package com.alan.citascritapp.ui

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.alan.citascritapp.R
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.ui.components.CitaCard
import com.alan.citascritapp.ui.components.EmptyState
import com.alan.citascritapp.utils.*
import com.alan.citascritapp.ui.theme.CritBlue
import com.alan.citascritapp.ui.theme.CritPurple
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppContent(
    viewModel: MainViewModel,
    onEditPerfil: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Estado local para el diálogo de permiso de alarmas (ahora controlado por VM)
    if (uiState.showAlarmPermissionDialog) {
        DialogoPermisoAlarmas(
            onDismiss = { viewModel.dismissAlarmPermissionDialog() },
            onConfirm = {
                viewModel.dismissAlarmPermissionDialog()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }
        )
    }
    val scope = rememberCoroutineScope()

    // Estados locales de UI
    var mostrarFinalizadas by remember { mutableStateOf(false) }
    var mostrarDialogoPermisoAlarma by remember { mutableStateOf(false) }
    var showDinoDialog by remember { mutableStateOf(false) }
    var showDialogSobrescribir by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    // TTS
    val ttsHelper = remember { TTSHelper(context) }
    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }

    // Launcher PDF
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val isPdf = uri.toString().endsWith(".pdf", ignoreCase = true) ||
                    context.contentResolver.getType(uri) == "application/pdf"
            if (!isPdf) {
                viewModel.mostrarBanner("El archivo seleccionado no es un PDF.", Color(0xFFD32F2F), Icons.Default.Error)
                return@rememberLauncherForActivityResult
            }
            
            if (uiState.citas.isNotEmpty()) {
                pendingUri = uri
                showDialogSobrescribir = true
            } else {
                viewModel.procesarPdfAgenda(uri)
            }
        }
    }

    // Dialog Sobrescribir
    if (showDialogSobrescribir && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showDialogSobrescribir = false; pendingUri = null },
            title = { Text("¿Sobrescribir citas?") },
            text = { Text("Al subir una nueva agenda se reemplazarán todas las citas y carnet guardados previamente. ¿Deseas continuar?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialogSobrescribir = false
                    pendingUri?.let { viewModel.procesarPdfAgenda(it) }
                    pendingUri = null
                }) { Text("Sí, continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showDialogSobrescribir = false; pendingUri = null }) { Text("Cancelar") }
            }
        )
    }

    // Dialog Permiso Alarma
    if (mostrarDialogoPermisoAlarma) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoPermisoAlarma = false },
            title = { Text("Permiso necesario para alarmas exactas") },
            text = {
                Text("Para que la app pueda avisarte exactamente a la hora de tus citas, necesitas otorgar el permiso de 'Alarmas exactas'.\n\n¿Quieres ir a la configuración?")
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoPermisoAlarma = false
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }) { Text("Ir a configuración") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoPermisoAlarma = false }) { Text("No, gracias") }
            }
        )
    }

    Scaffold(
        topBar = {
            // Header Ultra Compacto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CritBlue, CritPurple)
                        )
                    )
            ) {
                // Contenido del Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding() // Ajuste automático para status bar
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Padding mínimo
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Título y Dino
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(36.dp) // Ultra reducido
                                .clickable { showDinoDialog = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painterResource(id = R.drawable.ic_dino_notif),
                                    contentDescription = "Dino",
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        
                        // Texto en una sola línea
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "Hola ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = uiState.perfil?.nombre?.split(" ")?.firstOrNull() ?: "Amigo",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    // Foto de Perfil
                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.6f)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(36.dp) // Ultra reducido
                            .clickable { onEditPerfil() }
                    ) {
                        if (!uiState.perfil?.fotoPath.isNullOrBlank()) {
                            AsyncImage(
                                model = uiState.perfil!!.fotoPath,
                                contentDescription = "Perfil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = CritBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Resumen Perfil
            PerfilResumen(perfil = uiState.perfil, onEditPerfil = onEditPerfil)
            Spacer(Modifier.height(12.dp))

            // Botón Cargar PDF
            Button(
                onClick = { launcher.launch("application/pdf") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CritBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("Cargar nueva agenda PDF", style = MaterialTheme.typography.titleMedium)
            }
            
            // Switch Finalizadas
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, bottom = 6.dp)
            ) {
                Switch(
                    checked = mostrarFinalizadas,
                    onCheckedChange = { mostrarFinalizadas = it }
                )
                Spacer(Modifier.width(8.dp))
                Text("Mostrar citas finalizadas / Canceladas")
            }

            // Lista de Citas
            SwipeRefresh(
                state = rememberSwipeRefreshState(uiState.isLoading),
                onRefresh = { viewModel.cargarDatosIniciales(fromUser = true) }
            ) {
                val citasFiltradas = if (mostrarFinalizadas) {
                    uiState.citas
                } else {
                    uiState.citas.filter {
                        (!citaYaPaso(it.fecha, it.hora) || citaEnCurso(it.fecha, it.hora, it.servicio)) && !it.cancelada
                    }
                }

                if (citasFiltradas.isEmpty() && !uiState.isLoading) {
                    EmptyState(modifier = Modifier.weight(1f))
                } else {
                    val citasPorFecha = citasFiltradas.groupBy { it.fecha }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        citasPorFecha.forEach { (fecha, citasDeEseDia) ->
                            // Encabezado por fecha (Sticky)
                            stickyHeader {
                                Surface(
                                    color = Color(0xFFF6F6F6),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = fecha,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                                        if (puedeDeslizar && (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart)) {
                                            citaSeleccionada = cita
                                            abrirDialogo = true
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            false
                                        } else false
                                    }
                                )

                                // Animación de entrada (Modifier.animateItemPlacement() requiere FoundationApi, 
                                // pero como no estoy seguro de la versión de compose, usaré un wrapper simple si es necesario,
                                // o simplemente dejaré el swipe que ya tiene animación).
                                // Nota: animateItemPlacement es experimental.
                                
                                SwipeToDismissBox(
                                    state = dismissState,
                                    modifier = Modifier.animateItemPlacement(), // Animación de lista
                                    enableDismissFromEndToStart = puedeDeslizar,
                                    enableDismissFromStartToEnd = puedeDeslizar,
                                    backgroundContent = {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFFFFE0B2)),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.padding(end = 24.dp))
                                        }
                                    },
                                    content = {
                                        CitaCard(
                                            cita = cita,
                                            color = color,
                                            icon = icon,
                                            onClick = {
                                                val texto = "Cita de ${cita.servicio}, el ${cita.fecha}, a las ${cita.hora}, con ${cita.medico}"
                                                ttsHelper.speak(texto)
                                            }
                                        )
                                    }
                                )

                                if (abrirDialogo) {
                                    AlertDialog(
                                        onDismissRequest = { abrirDialogo = false },
                                        title = { Text("Marcar como Cancelada") },
                                        text = { Text("¿Deseas marcar esta cita como cancelada? Solo es informativo.") },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                viewModel.cancelarCita(cita)
                                                abrirDialogo = false
                                            }) { Text("Sí, marcar") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { abrirDialogo = false }) { Text("No") }
                                        }
                                    )
                                }
                            }
                            }

                        
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = "Total de citas: ${citasFiltradas.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog Dino
    if (showDinoDialog) {
        val ctx = LocalContext.current
        LaunchedEffect(showDinoDialog) {
            val mediaPlayer = MediaPlayer.create(ctx, R.raw.marimba_ritmo)
            mediaPlayer.start()
            mediaPlayer.setOnCompletionListener { it.release() }
        }
        AlertDialog(
            onDismissRequest = { showDinoDialog = false },
            confirmButton = { TextButton(onClick = { showDinoDialog = false }) { Text("Cerrar") } },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("¡Bailecito del Nino Nino! 🦕")
                    Spacer(Modifier.height(10.dp))
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data("file:///android_asset/dino_dance.gif")
                            .decoderFactory(GifDecoder.Factory())
                            .build(),
                        contentDescription = "Dino",
                        modifier = Modifier.size(250.dp)
                    )
                    Text("¡Con Cariño Alan Rodrigo! 💛💜")
                }
            }
        )
    }
}

@Composable
fun DialogoPermisoAlarmas(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permiso necesario para alarmas exactas") },
        text = {
            Text("Para que la app pueda avisarte exactamente a la hora de tus citas, necesitas otorgar el permiso de 'Alarmas exactas'.\n\n¿Quieres ir a la configuración?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Ir a configuración") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("No, gracias") }
        }
    )
}
