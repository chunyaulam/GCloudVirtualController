package com.singularitycode.gcloudvirtualcontroller.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.singularitycode.gcloudvirtualcontroller.data.DeviceDao
import com.singularitycode.gcloudvirtualcontroller.data.DeviceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

suspend fun checkDeviceOnline(ip: String, port: Int, timeoutMs: Int = 1000): Boolean = withContext(Dispatchers.IO) {
    var socket: DatagramSocket? = null
    try {
        socket = DatagramSocket()
        socket.soTimeout = timeoutMs
        val address = InetAddress.getByName(ip)

        // Send UDP Ping packet (Packet Type 0x00)
        val pingBuffer = ByteBuffer.allocate(5).apply {
            putInt(System.currentTimeMillis().toInt())
            put(0x00.toByte())
            flip()
        }
        val pingBytes = pingBuffer.array()
        val packet = DatagramPacket(pingBytes, pingBytes.size, address, port)
        socket.send(packet)

        // Must receive an explicit response packet from the server application!
        val receiveBuffer = ByteArray(256)
        val recvPacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
        socket.receive(recvPacket)

        // Successfully received response from server application
        true
    } catch (_: Exception) {
        // Timeout, PortUnreachableException, or network error -> Server receiver is offline
        false
    } finally {
        try {
            socket?.close()
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSelectionScreen(
    deviceDao: DeviceDao,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onDeviceSelected: (DeviceEntity) -> Unit
) {
    val savedDevices by deviceDao.getAllDevices().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var deviceToEdit by remember { mutableStateOf<DeviceEntity?>(null) }
    var deviceStatuses by remember { mutableStateOf<Map<Int, Boolean?>>(emptyMap()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var connectingDeviceId by remember { mutableStateOf<Int?>(null) }
    var isGridView by rememberSaveable { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun refreshStatuses() {
        if (savedDevices.isEmpty()) return
        scope.launch {
            isRefreshing = true
            deviceStatuses = savedDevices.associate { it.id to null }

            val results = savedDevices.map { device ->
                async {
                    device.id to checkDeviceOnline(device.ip, device.port)
                }
            }.awaitAll().toMap()

            deviceStatuses = results
            isRefreshing = false
        }
    }

    fun handleDeviceSelect(device: DeviceEntity) {
        if (connectingDeviceId != null) return

        val currentStatus = deviceStatuses[device.id]

        // 1. 若顯示 Offline 狀態，不再次 Ping 檢測，直接提示使用者點擊右上角 Refresh 按鈕
        if (currentStatus == false) {
            Toast.makeText(
                context,
                "${device.name} is offline. Tap top refresh button to re-check.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 2. 若正在檢測中，提示使用者等待
        if (currentStatus == null) {
            Toast.makeText(
                context,
                "Checking ${device.name} status...",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 3. 僅在顯示 Online 狀態時，點擊才進行二次即時確認
        scope.launch {
            connectingDeviceId = device.id
            val isStillOnline = checkDeviceOnline(device.ip, device.port, timeoutMs = 800)

            deviceStatuses = deviceStatuses + (device.id to isStillOnline)
            connectingDeviceId = null

            if (isStillOnline) {
                onDeviceSelected(device)
            } else {
                Toast.makeText(
                    context,
                    "Cannot connect: ${device.name} (${device.ip}:${device.port}) has gone offline",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    LaunchedEffect(savedDevices) {
        if (savedDevices.isNotEmpty()) {
            refreshStatuses()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GCloud Controller") },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                        )
                    }

                    IconButton(
                        onClick = { refreshStatuses() },
                        enabled = !isRefreshing && connectingDeviceId == null
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Device Status"
                            )
                        }
                    }

                    IconButton(onClick = onThemeToggle) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Device")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            Text(
                text = "Saved Devices",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (savedDevices.isNotEmpty()) {
                if (isGridView) {
                    // Grid / Card Layout (2 Columns)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        savedDevices.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { device ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        DeviceGridCard(
                                            device = device,
                                            isOnline = deviceStatuses[device.id],
                                            isConnecting = connectingDeviceId == device.id,
                                            onClick = { handleDeviceSelect(device) },
                                            onLongClick = { deviceToEdit = device }
                                        )
                                    }
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    // List Layout (Full Width Cards)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        savedDevices.forEach { device ->
                            DeviceCard(
                                device = device,
                                isOnline = deviceStatuses[device.id],
                                isConnecting = connectingDeviceId == device.id,
                                onClick = { handleDeviceSelect(device) },
                                onLongClick = { deviceToEdit = device }
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No devices saved. Click + to add.", color = Color.Gray)
                }
            }
        }

        if (showAddDialog) {
            DeviceEditDialog(
                title = "Add New Device",
                onDismiss = { showAddDialog = false },
                onConfirm = { name, ip, port ->
                    scope.launch {
                        if (deviceDao.exists(ip, port)) {
                            Toast.makeText(context, "Device with this IP and Port already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            deviceDao.insert(DeviceEntity(name = name, ip = ip, port = port))
                            showAddDialog = false
                        }
                    }
                }
            )
        }

        if (deviceToEdit != null) {
            val currentDevice = deviceToEdit!!
            DeviceEditDialog(
                title = "Edit Device",
                initialName = currentDevice.name,
                initialIp = currentDevice.ip,
                initialPort = currentDevice.port.toString(),
                isEdit = true,
                onDismiss = { deviceToEdit = null },
                onConfirm = { name, ip, port ->
                    scope.launch {
                        if (deviceDao.existsExcludingId(ip, port, currentDevice.id)) {
                            Toast.makeText(context, "Another device with this IP and Port already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            deviceDao.insert(currentDevice.copy(name = name, ip = ip, port = port))
                            deviceToEdit = null
                        }
                    }
                },
                onDelete = {
                    scope.launch {
                        deviceDao.delete(currentDevice)
                    }
                    deviceToEdit = null
                }
            )
        }
    }
}

@Composable
fun DeviceCard(
    device: DeviceEntity,
    isOnline: Boolean?,
    isConnecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOnline == true) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline == false) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${device.ip}:${device.port}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusBadge(isOnline = isOnline, isConnecting = isConnecting)
        }
    }
}

@Composable
fun DeviceGridCard(
    device: DeviceEntity,
    isOnline: Boolean?,
    isConnecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isOnline == true) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOnline == false) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${device.ip}:${device.port}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(isOnline = isOnline, isConnecting = isConnecting)
            }
        }
    }
}

@Composable
fun StatusBadge(isOnline: Boolean?, isConnecting: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Connecting...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        } else {
            when (isOnline) {
                true -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
                false -> {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFE53935), CircleShape)
                    )
                    Text(
                        text = "Offline",
                        fontSize = 12.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                }
                null -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Checking...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceEditDialog(
    title: String,
    initialName: String = "",
    initialIp: String = "",
    initialPort: String = "8567",
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && ip.isNotBlank()) {
                        onConfirm(name, ip, port.toIntOrNull() ?: 8567)
                    }
                }
            ) {
                Text(if (isEdit) "Save" else "Add")
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
