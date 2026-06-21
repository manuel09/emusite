package com.emusite.app.ui.screens

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emusite.app.data.MainViewModel
import com.emusite.app.server.RepoServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirestickRepoDialog(
    viewModel: MainViewModel = viewModel(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var serverUrl by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("Starting server...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val ip = getDeviceIp(context)
            if (ip != null) {
                val server = RepoServer(8080) { repoUrl ->
                    scope.launch { viewModel.fetchRepository(repoUrl) }
                }
                server.start()
                serverUrl = "http://$ip:8080"
                message = "Open this URL on your phone:"
            } else {
                message = "No network connection"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Repository") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium)
                if (serverUrl != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        serverUrl!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scan the QR code or open the URL on your phone. Paste the repository URL and submit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

private fun getDeviceIp(context: Context): String? {
    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val ip = wifiManager?.connectionInfo?.ipAddress ?: 0
        if (ip != 0) {
            return String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        }
    } catch (_: Exception) {}

    try {
        java.net.NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { intf ->
            if (intf.isLoopback || !intf.isUp) return@forEach
            intf.inetAddresses.toList().forEach { addr ->
                if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
    } catch (_: Exception) {}

    return null
}
