package com.alan.citascritapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun BannerNotification(
    message: String,
    icon: ImageVector = Icons.Default.Info,
    backgroundColor: Color = Color(0xFFD32F2F),
    onClose: () -> Unit = {}
) {
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(backgroundColor)
            .padding(horizontal = 0.dp, vertical = 0.dp)
            .zIndex(2f)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = message,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar banner",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun TimedBanner(
    message: String?,
    icon: ImageVector,
    backgroundColor: Color,
    onClose: () -> Unit,
    durationMillis: Long = 2000
) {
    if (message != null) {
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(durationMillis)
            onClose()
        }
        BannerNotification(
            message = message,
            icon = icon,
            backgroundColor = backgroundColor,
            onClose = onClose
        )
    }
}
