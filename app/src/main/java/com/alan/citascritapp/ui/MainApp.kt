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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainApp(context: Context) {
    var showPerfil by remember { mutableStateOf(false) }
    var perfil by remember { mutableStateOf<com.alan.citascritapp.models.PacienteProfile?>(null) }
    val scope = rememberCoroutineScope()

    // Banner notification state
    var bannerMessage by remember { mutableStateOf<String?>(null) }
    var bannerColor by remember { mutableStateOf(Color(0xFFD32F2F)) }
    var bannerIcon by remember { mutableStateOf<ImageVector?>(null) }

    // Cargar perfil en inicio
    LaunchedEffect(Unit) {
        perfil = cargarPerfil(context)
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
                    onPerfilChange = {
                        perfil = it
                    },
                    onGuardar = {
                        scope.launch {
                            guardarPerfil(context, perfil ?: com.alan.citascritapp.models.perfilVacio)
                            // Banner de éxito
                            bannerMessage = "Perfil actualizado correctamente"
                            bannerIcon = Icons.Default.CheckCircle
                            bannerColor = Color(0xFF388E3C)
                        }
                        showPerfil = false
                    },
                    onCancelar = {
                        showPerfil = false
                    }
                )
            } else {
                AppContent(
                    context = context,
                    perfil = perfil,
                    onEditPerfil = { showPerfil = true },
                    onPerfilUpdate = {
                        perfil = it
                        scope.launch { guardarPerfil(context, it) }
                        // Banner de éxito
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
            }
        }
    }
}
