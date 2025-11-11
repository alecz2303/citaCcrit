package com.alan.citascritapp.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alan.citascritapp.models.PacienteProfile
import com.alan.citascritapp.utils.toEdad
import java.io.File

@Composable
fun PerfilResumen(
    perfil: PacienteProfile?,
    onEditPerfil: () -> Unit // 🚀 Nuevo parámetro
) {
    // Estados de UI
    var mostrarFotoPopup by remember { mutableStateOf(false) }
    var mostrarDialogoSinFoto by remember { mutableStateOf(false) }

    // FOTO (clicable)
    val tieneFoto = perfil?.fotoPath?.isNotBlank() == true && File(perfil.fotoPath).exists()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 16.dp, horizontal = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .clickable {
                    if (tieneFoto) mostrarFotoPopup = true
                    else mostrarDialogoSinFoto = true
                },
            contentAlignment = Alignment.Center
        ) {
            if (tieneFoto) {
                val bitmap = remember(perfil?.fotoPath) {
                    BitmapFactory.decodeFile(perfil?.fotoPath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto paciente",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(42.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(42.dp)
                )
            }
        }

        Spacer(Modifier.width(20.dp))

        // DATOS DEL PERFIL
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Nombre completo
            Text(
                text = listOfNotNull(
                    perfil?.nombre?.takeIf { it.isNotBlank() },
                    perfil?.apellidoP?.takeIf { it.isNotBlank() },
                    perfil?.apellidoM?.takeIf { it.isNotBlank() }
                ).joinToString(" ").ifEmpty { "Sin nombre" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            // Edad
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Edad: ${perfil?.fechaNacimiento?.toEdad() ?: "--"} años",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Carnet
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Badge,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Carnet: ${perfil?.carnet ?: "--"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // --- POPUP con la foto ampliada ---
    if (mostrarFotoPopup && tieneFoto) {
        AlertDialog(
            onDismissRequest = { mostrarFotoPopup = false },
            confirmButton = {
                TextButton(onClick = { mostrarFotoPopup = false }) {
                    Text("Cerrar")
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val bitmap = remember(perfil?.fotoPath) {
                        BitmapFactory.decodeFile(perfil?.fotoPath)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Foto ampliada",
                            modifier = Modifier
                                .size(280.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Foto de perfil",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    // --- DIALOGO cuando no hay foto ---
    if (mostrarDialogoSinFoto) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSinFoto = false },
            title = { Text("No hay foto de perfil") },
            text = { Text("Aún no tienes una foto guardada. ¿Deseas agregar una ahora?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoSinFoto = false
                    onEditPerfil() // 🚀 Abre pantalla de perfil
                }) {
                    Text("Sí, agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSinFoto = false }) {
                    Text("No")
                }
            }
        )
    }
}
