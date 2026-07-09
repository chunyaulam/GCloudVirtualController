package com.singularitycode.gcloudvirtualcontroller

import androidx.compose.ui.geometry.Offset

data class GamepadState(
    val pressedButtons: Set<Int> = emptySet(),
    val leftJoystick: Offset = Offset.Zero,
    val rightJoystick: Offset = Offset.Zero,
    val dpad: Offset = Offset.Zero,
    val l2: Float = 0f,
    val r2: Float = 0f,
    val isConnected: Boolean = false
)
