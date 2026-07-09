package com.singularitycode.gcloudvirtualcontroller

import android.view.KeyEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GamepadRemoteController(private val udpManager: UdpSenderManager) {

    /**
     * Packet Structure (17 Bytes):
     * - Timestamp: UInt32 (4 Bytes)
     * - Packet Type: UInt8 (1 Byte) - Fixed 0x03
     * - Buttons Bitmask: UInt16 (2 Bytes)
     * - Left Stick X: Int16 (2 Bytes)
     * - Left Stick Y: Int16 (2 Bytes)
     * - Right Stick X: Int16 (2 Bytes)
     * - Right Stick Y: Int16 (2 Bytes)
     * - Left Trigger: UInt8 (1 Byte)
     * - Right Trigger: UInt8 (1 Byte)
     */
    fun sendState(state: GamepadState) {
        val buffer = ByteBuffer.allocate(17)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // 1. Timestamp (UInt32, 4 Bytes)
        val timestamp = (System.currentTimeMillis() and 0xFFFFFFFFL).toInt()
        buffer.putInt(timestamp)

        // 2. Packet Type (UInt8, 1 Byte) - Fixed 0x03
        buffer.put(0x03.toByte())

        // 3. Buttons Bitmask (UInt16, 2 Bytes)
        var bitmask = 0
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_A)) bitmask = bitmask or (1 shl 0)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_B)) bitmask = bitmask or (1 shl 1)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_X)) bitmask = bitmask or (1 shl 2)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_Y)) bitmask = bitmask or (1 shl 3)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_L1)) bitmask = bitmask or (1 shl 4)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_R1)) bitmask = bitmask or (1 shl 5)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_THUMBL)) bitmask = bitmask or (1 shl 6)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_THUMBR)) bitmask = bitmask or (1 shl 7)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_START)) bitmask = bitmask or (1 shl 8)
        if (state.pressedButtons.contains(KeyEvent.KEYCODE_BUTTON_SELECT)) bitmask = bitmask or (1 shl 9)
        
        // D-pad handling from Offset
        if (state.dpad.y < -0.5f) bitmask = bitmask or (1 shl 10) // Up
        if (state.dpad.y > 0.5f) bitmask = bitmask or (1 shl 11) // Down
        if (state.dpad.x < -0.5f) bitmask = bitmask or (1 shl 12) // Left
        if (state.dpad.x > 0.5f) bitmask = bitmask or (1 shl 13) // Right
        
        buffer.putShort(bitmask.toShort())

        // 4-7. Joysticks (Int16, 2 Bytes each) - Convert -1.0~1.0 to -32768~32767
        // We use coerceIn to ensure safety
        buffer.putShort((state.leftJoystick.x.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        buffer.putShort((state.leftJoystick.y.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        buffer.putShort((state.rightJoystick.x.coerceIn(-1f, 1f) * 32767).toInt().toShort())
        buffer.putShort((state.rightJoystick.y.coerceIn(-1f, 1f) * 32767).toInt().toShort())

        // 8-9. Triggers (UInt8, 1 Byte each) - Convert 0~1.0 to 0~255
        buffer.put((state.l2.coerceIn(0f, 1f) * 255).toInt().toByte())
        buffer.put((state.r2.coerceIn(0f, 1f) * 255).toInt().toByte())

        buffer.flip()
        udpManager.sendData(buffer)
    }
}
