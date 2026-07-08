package com.singularitycode.gcloudvirtualcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.singularitycode.gcloudvirtualcontroller.data.AppDatabase
import com.singularitycode.gcloudvirtualcontroller.data.DeviceEntity
import com.singularitycode.gcloudvirtualcontroller.ui.screens.DeviceSelectionScreen
import com.singularitycode.gcloudvirtualcontroller.ui.screens.TrackpadScreen
import com.singularitycode.gcloudvirtualcontroller.ui.theme.GCloudVirtualControllerTheme

class MainActivity : ComponentActivity() {

    private var mouseController: MouseRemoteController? = null
    private var udpManager: UdpSenderManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed enableEdgeToEdge() to keep layout stable

        val db = AppDatabase.getDatabase(this)
        val deviceDao = db.deviceDao()

        setContent {
            var selectedDevice by remember { mutableStateOf<DeviceEntity?>(null) }
            var isDarkTheme by remember { mutableStateOf(true) }

            // Handle Full Screen / System UI Visibility
            LaunchedEffect(selectedDevice) {
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                if (selectedDevice != null) {
                    // Hide System Bars for Trackpad
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    // Show System Bars for Device Selection
                    windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            GCloudVirtualControllerTheme(darkTheme = isDarkTheme) {
                if (selectedDevice == null) {
                    DeviceSelectionScreen(
                        deviceDao = deviceDao,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme },
                        onDeviceSelected = { device ->
                            udpManager?.close()
                            val manager = UdpSenderManager(device.ip, device.port)
                            udpManager = manager
                            mouseController = MouseRemoteController(manager)
                            selectedDevice = device
                        }
                    )
                } else {
                    TrackpadScreen(
                        mouseController = mouseController,
                        isDarkTheme = isDarkTheme,
                        onBack = {
                            selectedDevice = null
                            udpManager?.close()
                            udpManager = null
                            mouseController = null
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        udpManager?.close()
    }
}
