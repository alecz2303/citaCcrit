package com.alan.citascritapp.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.models.PacienteProfile
import com.alan.citascritapp.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val citas: List<Cita> = emptyList(),
    val perfil: PacienteProfile? = null,
    val carnet: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bannerMessage: String? = null,
    val bannerColor: Color = Color.Red,
    val bannerIcon: ImageVector? = null,
    val onboardingCompleted: Boolean = false,
    val showAlarmPermissionDialog: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // Context helper (be careful with leaks, using application context is safe here for utils)
    private val context: Context get() = getApplication<Application>().applicationContext

    init {
        cargarDatosIniciales()
    }

    fun cargarDatosIniciales(fromUser: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val perfil = cargarPerfil(context)
                val citas = cargarCitas(context)
                val carnet = cargarCarnet(context)
                
                // Cargar onboarding (Flow -> first())
                val onboarding = cargarOnboarding(context).firstOrNull() ?: false
                
                _uiState.update { 
                    it.copy(
                        perfil = perfil,
                        citas = citas,
                        carnet = carnet,
                        onboardingCompleted = onboarding,
                        isLoading = false
                    )
                }
                
                // Reprogramar alarmas si es necesario (lógica existente)
                recargarAlarmas(citas)
                
                if (fromUser) {
                    mostrarBanner("Datos actualizados correctamente", Color(0xFF388E3C), Icons.Default.CheckCircle)
                }
                
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al cargar datos: ${e.message}") }
            }
        }
    }

    fun completarOnboarding() {
        viewModelScope.launch {
            guardarOnboarding(context, true)
            _uiState.update { it.copy(onboardingCompleted = true) }
        }
    }
    
    fun dismissAlarmPermissionDialog() {
        _uiState.update { it.copy(showAlarmPermissionDialog = false) }
    }

    private fun recargarAlarmas(citas: List<Cita>) {
        citas.forEach { cita ->
            if (!citaYaPaso(cita.fecha, cita.hora) && !cita.cancelada) {
                programarAlarmaCita(context, cita, onPermisoFaltante = { 
                    _uiState.update { it.copy(showAlarmPermissionDialog = true) }
                })
            }
        }
        programarAlertasCitasDiaSiguiente(context, citas)
    }

    fun actualizarPerfil(nuevoPerfil: PacienteProfile) {
        viewModelScope.launch {
            guardarPerfil(context, nuevoPerfil)
            _uiState.update { it.copy(perfil = nuevoPerfil) }
            mostrarBanner("Perfil actualizado correctamente", Color(0xFF388E3C), Icons.Default.CheckCircle)
        }
    }

    fun procesarPdfAgenda(uri: Uri) {
        val perfil = uiState.value.perfil
        if (perfil == null || perfil.carnet.isBlank()) {
            mostrarBanner("Completa tu perfil antes de subir la agenda.", Color(0xFFF9A825), Icons.Default.Person)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Usamos la función existente procesarPDF pero adaptada o llamando sus partes
                // Nota: procesarPDF en utils actualmente hace todo junto. Idealmente se refactorizaría,
                // pero por ahora podemos llamar a las funciones de extracción directamente si son públicas
                // o usar procesarPDF pasando un scope.
                
                // Para mantener consistencia con el código existente, simularemos el flujo:
                val texto = extractTextFromPDF(context, uri)
                val nuevoCarnet = extraerCarnet(texto)
                
                if (nuevoCarnet == null) {
                    mostrarBanner("El archivo no es válido.", Color(0xFFD32F2F), Icons.Default.Warning)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                
                if (nuevoCarnet != perfil.carnet) {
                    mostrarBanner("El carnet no coincide con el perfil.", Color(0xFFD32F2F), Icons.Default.Warning)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // Cancelar alarmas viejas
                cancelarTodasAlarmasCitas(context)

                // Extraer y guardar nuevas citas
                val nuevasCitas = extraerCitas(texto)
                guardarCitas(context, nuevasCitas)
                guardarCarnet(context, nuevoCarnet)

                // Actualizar estado
                _uiState.update { 
                    it.copy(
                        citas = nuevasCitas, 
                        carnet = nuevoCarnet, 
                        isLoading = false 
                    ) 
                }
                
                recargarAlarmas(nuevasCitas)
                mostrarBanner("Agenda cargada correctamente.", Color(0xFF388E3C), Icons.Default.CheckCircle)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error procesando PDF", e)
                mostrarBanner("Error al procesar el archivo.", Color(0xFFD32F2F), Icons.Default.Error)
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun cancelarCita(cita: Cita) {
        viewModelScope.launch {
            cancelarAlarmaCita(context, cita)
            val nuevasCitas = uiState.value.citas.map {
                if (it == cita) it.copy(cancelada = true) else it
            }
            guardarCitas(context, nuevasCitas)
            _uiState.update { it.copy(citas = nuevasCitas) }
        }
    }

    fun mostrarBanner(msg: String, color: Color, icon: ImageVector?) {
        _uiState.update { it.copy(bannerMessage = msg, bannerColor = color, bannerIcon = icon) }
    }

    fun ocultarBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }
}
