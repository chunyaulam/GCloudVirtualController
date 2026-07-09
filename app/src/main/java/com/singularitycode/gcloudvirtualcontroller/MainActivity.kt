package com.singularitycode.gcloudvirtualcontroller

import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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
    private var gamepadController: GamepadRemoteController? = null
    private var keyboardController: KeyboardRemoteController? = null
    private var udpManager: UdpSenderManager? = null
    private var isInTrackpadMode = false
    private var gamepadState by mutableStateOf(GamepadState())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Removed enableEdgeToEdge() to keep layout stable

        val db = AppDatabase.getDatabase(this)
        val deviceDao = db.deviceDao()

        setContent {
            var selectedDevice by remember { mutableStateOf<DeviceEntity?>(null) }
            var isDarkTheme by remember { mutableStateOf(true) }

            // Monitor Gamepad connection
            LaunchedEffect(Unit) {
                checkGamepadConnection()
            }

            // Handle Full Screen / System UI Visibility
            LaunchedEffect(selectedDevice) {
                isInTrackpadMode = selectedDevice != null
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
                            gamepadController = GamepadRemoteController(manager)
                            keyboardController = KeyboardRemoteController(manager)
                            selectedDevice = device
                        }
                    )
                } else {
                    TrackpadScreen(
                        mouseController = mouseController,
                        keyboardController = keyboardController,
                        isDarkTheme = isDarkTheme,
                        gamepadState = gamepadState,
                        onBack = {
                            selectedDevice = null
                            udpManager?.close()
                            udpManager = null
                            mouseController = null
                            gamepadController = null
                            keyboardController = null
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
        if (isInTrackpadMode && (event.source and android.view.InputDevice.SOURCE_GAMEPAD == android.view.InputDevice.SOURCE_GAMEPAD || 
            event.source and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK)) {
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
        if (isInTrackpadMode && (event.source and android.view.InputDevice.SOURCE_GAMEPAD == android.view.InputDevice.SOURCE_GAMEPAD || 
            event.source and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK)) {
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
        if (isInTrackpadMode && (event.source and android.view.InputDevice.SOURCE_JOYSTICK == android.view.InputDevice.SOURCE_JOYSTICK)) {
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
