package com.alan.citascritapp.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.citascritapp.models.PacienteProfile
import com.alan.citascritapp.ui.theme.CritBlue
import com.alan.citascritapp.ui.theme.CritPurple
import com.alan.citascritapp.utils.toEdad
import java.io.File

@Composable
fun PerfilResumen(
    perfil: PacienteProfile?,
    onEditPerfil: () -> Unit
) {
    // Estados de UI
    var mostrarFotoPopup by remember { mutableStateOf(false) }
    var mostrarDialogoSinFoto by remember { mutableStateOf(false) }

    // FOTO (clicable)
    val tieneFoto = perfil?.fotoPath?.isNotBlank() == true && File(perfil.fotoPath).exists()

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(CritBlue, CritPurple)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // FOTO DE PERFIL
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
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
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(20.dp))

                // DATOS DEL PERFIL
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Nombre completo
                    Text(
                        text = listOfNotNull(
                            perfil?.nombre?.takeIf { it.isNotBlank() },
                            perfil?.apellidoP?.takeIf { it.isNotBlank() },
                            perfil?.apellidoM?.takeIf { it.isNotBlank() }
                        ).joinToString(" ").ifEmpty { "Sin nombre" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = Color.White
                    )

                    // Edad
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Edad: ${perfil?.fechaNacimiento?.toEdad() ?: "--"} años",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    // Carnet
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Badge,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Carnet: ${perfil?.carnet ?: "--"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // Icono Editar (Sutil)
                IconButton(onClick = onEditPerfil) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
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
