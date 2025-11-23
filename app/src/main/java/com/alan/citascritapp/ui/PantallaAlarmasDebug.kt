package com.alan.citascritapp.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alan.citascritapp.utils.obtenerAlarmasDebug
import com.alan.citascritapp.utils.limpiarAlarmasDebug

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAlarmasDebug(
    context: Context,
    onBack: () -> Unit
) {
    var alarmas by remember { mutableStateOf(obtenerAlarmasDebug(context).toList()) }

    // Recarga la lista al volver a la pantalla
    LaunchedEffect(Unit) {
        alarmas = obtenerAlarmasDebug(context).toList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarmas Programadas (debug)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (alarmas.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No hay alarmas programadas.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        items(alarmas) { alarma ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Text(
                                    text = alarma,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        val intent = android.content.Intent(context, com.alan.citascritapp.broadcasts.AlarmaReceiver::class.java).apply {
                            putExtra("tipo", "alerta_citas_dia_siguiente")
                            putExtra("fecha", "Mañana (Prueba)")
                        }
                        context.sendBroadcast(intent)
                        android.widget.Toast.makeText(context, "Alerta de prueba enviada", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Probar Alerta Día Anterior")
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        limpiarAlarmasDebug(context)
                        alarmas = emptyList() // Limpiar lista local
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Limpiar alarmas debug")
                }
            }
        }
    )
}
