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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alan.citascritapp.ui.AppContent
import com.alan.citascritapp.ui.OnboardingScreen
import com.alan.citascritapp.ui.PerfilScreen
import com.alan.citascritapp.ui.TimedBanner
import com.alan.citascritapp.utils.guardarPerfil
import com.alan.citascritapp.models.perfilVacio
import kotlinx.coroutines.launch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.BugReport
import com.alan.citascritapp.ui.PantallaAlarmasDebug
import com.alan.citascritapp.BuildConfig

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainApp(context: Context) {
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    
    var showPerfil by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var mostrarAlarmasDebug by remember { mutableStateOf(false) }

    // Mostrar perfil si falta carnet
    LaunchedEffect(uiState.perfil) {
        if (uiState.perfil != null && uiState.perfil!!.carnet.isBlank()) {
            showPerfil = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Banner superior
        TimedBanner(
            message = uiState.bannerMessage,
            icon = uiState.bannerIcon ?: Icons.Default.Warning,
            backgroundColor = uiState.bannerColor,
            onClose = { viewModel.ocultarBanner() },
            durationMillis = 2000
        )

        Surface(color = MaterialTheme.colorScheme.background) {
            if (!uiState.onboardingCompleted && !uiState.isLoading) {
                OnboardingScreen(
                    onFinish = { viewModel.completarOnboarding() }
                )
            } else if (showPerfil) {
                PerfilScreen(
                    context = context,
                    perfil = uiState.perfil,
                    onPerfilChange = { nuevoPerfil -> 
                        viewModel.actualizarPerfil(nuevoPerfil)
                    },
                    onGuardar = {
                        showPerfil = false
                    },
                    onCancelar = {
                        if (!uiState.perfil?.carnet.isNullOrBlank()) showPerfil = false
                    },
                    ocultarCancelar = uiState.perfil?.carnet.isNullOrBlank()
                )
            } else if (mostrarAlarmasDebug) {
                PantallaAlarmasDebug(
                    context = context,
                    onBack = { mostrarAlarmasDebug = false }
                )
            } else {
                AppContent(
                    viewModel = viewModel,
                    onEditPerfil = { showPerfil = true }
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
