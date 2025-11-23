package com.alan.citascritapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alan.citascritapp.models.Cita
import com.alan.citascritapp.utils.DuracionesTerapias
import com.alan.citascritapp.utils.citaEnCurso
import com.alan.citascritapp.utils.citaYaPaso
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CitaCard(
    cita: Cita,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.elevatedCardColors(containerColor = color),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            Row(
                Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icono Circular con fondo
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    // Servicio (Título Principal)
                    Text(
                        text = cita.servicio,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(Modifier.height(4.dp))

                    // Hora y Chips
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            val duracion = DuracionesTerapias.obtenerDuracion(cita.servicio)
                            Text(
                                text = "${cita.hora} • ${duracion} min",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    }

                    Spacer(Modifier.height(8.dp))

                    // SECCIÓN DE ESTATUS (Nueva División)
                    val (statusText, statusColor, statusIcon) = when {
                        cita.cancelada -> Triple("Cancelada", Color(0xFFE65100), Icons.Default.EventBusy)
                        citaEnCurso(cita.fecha, cita.hora, cita.servicio) -> {
                            val duracion = DuracionesTerapias.obtenerDuracion(cita.servicio)
                            val horaFin = calcularHoraFin(cita.hora, duracion)
                            Triple("En Curso • Termina $horaFin", Color(0xFF2E7D32), Icons.Default.PlayCircle)
                        }
                        citaYaPaso(cita.fecha, cita.hora) -> Triple("Finalizada", Color(0xFFD32F2F), Icons.Default.CheckCircle)
                        else -> Triple("", Color.Transparent, null)
                    }

                    if (statusIcon != null) {
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        Spacer(Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = statusColor
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                    }


                    Spacer(Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))

                    // Detalles: Médico y Cubículo
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Médico",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = cita.medico,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Cubículo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = cita.cubiculo,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Aviso Cancelada
                    if (cita.cancelada) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Cita cancelada por el CRIT",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE65100),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    containerColor: Color,
    icon: ImageVector
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun calcularHoraFin(horaInicio: String, duracionMinutos: Int): String {
    return try {
        // Normalizar hora (eliminar puntos, espacios extra)
        var horaLimpia = horaInicio.lowercase()
            .replace(".", "")
            .replace("  ", " ")
            .trim()
        
        // Convertir a formato estándar AM/PM
        horaLimpia = horaLimpia.replace(" a m", " AM").replace(" p m", " PM")
        
        // Formato esperado: "10:00 AM"
        val formatoEntrada = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val fechaInicio = formatoEntrada.parse(horaLimpia)
        
        if (fechaInicio != null) {
            val cal = Calendar.getInstance()
            cal.time = fechaInicio
            cal.add(Calendar.MINUTE, duracionMinutos)
            
            val formatoSalida = SimpleDateFormat("h:mm a", Locale.ENGLISH)
            formatoSalida.format(cal.time).uppercase().replace("AM", "a.m.").replace("PM", "p.m.")
        } else {
            "--:--"
        }
    } catch (e: Exception) {
        "--:--"
    }
}
