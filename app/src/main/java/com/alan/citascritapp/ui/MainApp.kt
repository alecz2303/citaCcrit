package com.alan.citascritapp.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.alan.citascritapp.ui.AppContent
import com.alan.citascritapp.ui.PerfilScreen
import com.alan.citascritapp.ui.TimedBanner
import com.alan.citascritapp.utils.cargarPerfil
import com.alan.citascritapp.utils.guardarPerfil
import com.alan.citascritapp.models.PacienteProfile
import com.alan.citascritapp.models.perfilVacio
import kotlinx.coroutines.launch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.BugReport
import com.alan.citascritapp.ui.PantallaAlarmasDebug
import com.alan.citascritapp.BuildConfig // <-- Este import sí va

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainApp(context: Context) {
    var showPerfil by remember { mutableStateOf(false) }
    var perfil by remember { mutableStateOf<com.alan.citascritapp.models.PacienteProfile?>(null) }
    val scope = rememberCoroutineScope()

    var bannerMessage by remember { mutableStateOf<String?>(null) }
    var bannerColor by remember { mutableStateOf(Color(0xFFD32F2F)) }
    var bannerIcon by remember { mutableStateOf<ImageVector?>(null) }

    var faltaCarnet by remember { mutableStateOf(false) }
    var mostrarAlarmasDebug by remember { mutableStateOf(false) }

    // Cargar perfil en inicio
    LaunchedEffect(Unit) {
        val p = cargarPerfil(context)
        perfil = p
        if (p?.carnet.isNullOrBlank()) {
            showPerfil = true
            faltaCarnet = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Banner superior
        TimedBanner(
            message = bannerMessage,
            icon = bannerIcon ?: Icons.Default.Warning,
            backgroundColor = bannerColor,
            onClose = { bannerMessage = null },
            durationMillis = 2000
        )

        Surface(color = MaterialTheme.colorScheme.background) {
            if (showPerfil) {
                PerfilScreen(
                    context = context,
                    perfil = perfil,
                    onPerfilChange = { perfil = it },
                    onGuardar = {
                        if (perfil?.carnet.isNullOrBlank()) {
                            // Si carnet sigue vacío, no dejar salir y mostrar error
                            bannerMessage = "El campo Carnet es obligatorio para continuar."
                            bannerIcon = Icons.Default.Warning
                            bannerColor = Color(0xFFD32F2F)
                        } else {
                            scope.launch {
                                guardarPerfil(context, perfil ?: com.alan.citascritapp.models.perfilVacio)
                                bannerMessage = "Perfil actualizado correctamente"
                                bannerIcon = Icons.Default.CheckCircle
                                bannerColor = Color(0xFF388E3C)
                            }
                            showPerfil = false
                            faltaCarnet = false
                        }
                    },
                    onCancelar = {
                        // Solo dejar cancelar si no falta Carnet (es decir, ya había uno guardado antes)
                        if (!faltaCarnet) showPerfil = false
                    },
                    ocultarCancelar = faltaCarnet // <- pásale esto a tu PerfilScreen para ocultar Cancelar si falta Carnet
                )
            } else if (mostrarAlarmasDebug) {
                PantallaAlarmasDebug(
                    context = context,
                    onBack = { mostrarAlarmasDebug = false }
                )
            } else {
                AppContent(
                    context = context,
                    perfil = perfil,
                    onEditPerfil = { showPerfil = true },
                    onPerfilUpdate = {
                        perfil = it
                        scope.launch { guardarPerfil(context, it) }
                        bannerMessage = "Perfil actualizado correctamente"
                        bannerIcon = Icons.Default.CheckCircle
                        bannerColor = Color(0xFF388E3C)
                    },
                    showBanner = { msg, color, icon ->
                        bannerMessage = msg
                        bannerColor = color
                        bannerIcon = icon
                    }
                )
                // FAB SOLO EN DEBUG
                if (BuildConfig.DEBUG) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.BottomEnd
                    ) {
                        FloatingActionButton(
                            onClick = { mostrarAlarmasDebug = true },
                            containerColor = MaterialTheme.colorScheme.secondary
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = "Ver alarmas (debug)")
                        }
                    }
                }
            }
        }
    }
}
