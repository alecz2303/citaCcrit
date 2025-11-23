package com.alan.citascritapp.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alan.citascritapp.R
import com.alan.citascritapp.ui.theme.CritBlue
import com.alan.citascritapp.ui.theme.CritPurple
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            OnboardingPage(page = page)
        }

        // Indicadores y Botón
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicadores de página
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { iteration ->
                    val color = if (pagerState.currentPage == iteration) CritBlue else Color.LightGray
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            // Botón Siguiente / Comenzar
            Button(
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinish()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CritBlue)
            ) {
                Text(if (pagerState.currentPage == 2) "Comenzar" else "Siguiente")
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (imageRes, title, description) = when (page) {
            0 -> Triple(
                0, // Placeholder, no se usa
                "Bienvenido a Citas CRIT",
                "Gestiona tus citas médicas de forma fácil y rápida. Nunca más olvides una terapia."
            )
            1 -> Triple(
                0, // Placeholder
                "Sube tu Agenda",
                "Descarga tu agenda en PDF y súbela aquí. Nosotros organizamos todo por ti."
            )
            else -> Triple(
                0, // Placeholder
                "Recordatorios Automáticos",
                "Recibe notificaciones antes de tus citas y alertas un día antes. ¡Tu tranquilidad es primero!"
            )
        }

        // Imagen Ilustrativa
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(CritPurple.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (page) {
                0 -> Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = CritBlue
                )
                1 -> Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.UploadFile,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = CritBlue
                )
                2 -> Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Alarm,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = CritBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CritBlue,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
