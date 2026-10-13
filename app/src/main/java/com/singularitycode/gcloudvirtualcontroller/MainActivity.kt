package com.singularitycode.gcloudvirtualcontroller

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.singularitycode.gcloudvirtualcontroller.data.AppDatabase
import com.singularitycode.gcloudvirtualcontroller.ui.screens.DeviceSelectionScreen
import com.singularitycode.gcloudvirtualcontroller.ui.screens.TrackpadScreen
import com.singularitycode.gcloudvirtualcontroller.ui.theme.GCloudVirtualControllerTheme

class MainActivity : ComponentActivity() {

    private var mouseController: MouseRemoteController? = null
    private var gamepadController: GamepadRemoteController? = null
    private var keyboardController: KeyboardRemoteController? = null
    private var udpManager: UdpSenderManager? = null
    private var isInTrackpadMode = false
    private var gamepadState by mutableStateOf(GamepadState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabase.getDatabase(this)
        val deviceDao = db.deviceDao()

        setContent {
            var selectedDeviceId by rememberSaveable { mutableStateOf<Int?>(null) }
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            var disconnectReason by remember { mutableStateOf<String?>(null) }

            val savedDevices by deviceDao.getAllDevices().collectAsState(initial = emptyList())
            val selectedDevice = remember(savedDevices, selectedDeviceId) {
                savedDevices.find { it.id == selectedDeviceId }
            }

            // Monitor Gamepad connection & hot-plugging
            DisposableEffect(Unit) {
                val inputManager = getSystemService(InputManager::class.java)
                val listener = object : InputManager.InputDeviceListener {
                    override fun onInputDeviceAdded(deviceId: Int) = checkGamepadConnection()
                    override fun onInputDeviceRemoved(deviceId: Int) = checkGamepadConnection()
                    override fun onInputDeviceChanged(deviceId: Int) = checkGamepadConnection()
                }
                inputManager.registerInputDeviceListener(listener, null)
                checkGamepadConnection()
                onDispose {
                    inputManager.unregisterInputDeviceListener(listener)
                }
            }

            // Handle UDP Manager & Full Screen / System UI Visibility
            LaunchedEffect(selectedDevice) {
                isInTrackpadMode = selectedDevice != null
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

                if (selectedDevice != null) {
                    udpManager?.close()
                    val manager = UdpSenderManager(selectedDevice.ip, selectedDevice.port)
                    manager.onDisconnected = { reason ->
                        runOnUiThread {
                            disconnectReason = reason
                        }
                    }
                    udpManager = manager
                    mouseController = MouseRemoteController(manager)
                    gamepadController = GamepadRemoteController(manager)
                    keyboardController = KeyboardRemoteController(manager)

                    // Hide System Bars for Trackpad
                    windowInsetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    udpManager?.close()
                    udpManager = null
                    mouseController = null
                    gamepadController = null
                    keyboardController = null

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
                            disconnectReason = null
                            selectedDeviceId = device.id
                        }
                    )
                } else {
                    TrackpadScreen(
                        mouseController = mouseController,
                        keyboardController = keyboardController,
                        isDarkTheme = isDarkTheme,
                        gamepadState = gamepadState,
                        onBack = {
                            selectedDeviceId = null
                        }
                    )
                }

                if (disconnectReason != null) {
                    AlertDialog(
                        onDismissRequest = {
                            disconnectReason = null
                            selectedDeviceId = null
                        },
                        title = { Text("Server Disconnected") },
                        text = {
                            Text(
                                "Disconnected from ${selectedDevice?.name ?: "server"} (${selectedDevice?.ip}:${selectedDevice?.port}).\n\n$disconnectReason"
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    disconnectReason = null
                                    selectedDeviceId = null
                                }
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun checkGamepadConnection() {
        val deviceIds = InputDevice.getDeviceIds()
        var connected = false
        for (deviceId in deviceIds) {
            val device = InputDevice.getDevice(deviceId)
            val sources = device?.sources ?: 0
            if (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                connected = true
                break
            }
        }
        gamepadState = gamepadState.copy(isConnected = connected)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isInTrackpadMode && (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)) {
            val newState = gamepadState.copy(
                pressedButtons = gamepadState.pressedButtons + keyCode,
                isConnected = true
            )
            gamepadState = newState
            gamepadController?.sendState(newState)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isInTrackpadMode && (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
            event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)) {
            val newState = gamepadState.copy(
                pressedButtons = gamepadState.pressedButtons - keyCode,
                isConnected = true
            )
            gamepadState = newState
            gamepadController?.sendState(newState)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (isInTrackpadMode && (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)) {
            // Joysticks
            val axisX = event.getAxisValue(MotionEvent.AXIS_X)
            val axisY = event.getAxisValue(MotionEvent.AXIS_Y)
            val axisZ = event.getAxisValue(MotionEvent.AXIS_Z)
            val axisRZ = event.getAxisValue(MotionEvent.AXIS_RZ)

            // D-Pad (Hat Axis)
            val dpadX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val dpadY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

            // Triggers (L2/R2)
            val triggerL2 = event.getAxisValue(MotionEvent.AXIS_BRAKE)
            val triggerR2 = event.getAxisValue(MotionEvent.AXIS_GAS)
            val triggerL2Alt = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            val triggerR2Alt = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)

            val finalL2 = if (triggerL2 != 0f) triggerL2 else triggerL2Alt
            val finalR2 = if (triggerR2 != 0f) triggerR2 else triggerR2Alt

            val newState = gamepadState.copy(
                leftJoystick = Offset(axisX, axisY),
                rightJoystick = Offset(axisZ, axisRZ),
                dpad = Offset(dpadX, dpadY),
                l2 = finalL2,
                r2 = finalR2,
                isConnected = true
            )
            gamepadState = newState
            gamepadController?.sendState(newState)

            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onDestroy() {
        super.onDestroy()
        udpManager?.close()
    }
}
