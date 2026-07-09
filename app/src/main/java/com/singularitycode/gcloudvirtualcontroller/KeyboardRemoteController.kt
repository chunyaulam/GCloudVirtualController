package com.singularitycode.gcloudvirtualcontroller

import android.view.KeyEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder

class KeyboardRemoteController(private val udpManager: UdpSenderManager) {

    private var lastTimestamp = 0L

    private val androidToMacKeyMap = mapOf(
        KeyEvent.KEYCODE_A to 0,
        KeyEvent.KEYCODE_B to 11,
        KeyEvent.KEYCODE_C to 8,
        KeyEvent.KEYCODE_D to 2,
        KeyEvent.KEYCODE_E to 14,
        KeyEvent.KEYCODE_F to 3,
        KeyEvent.KEYCODE_G to 5,
        KeyEvent.KEYCODE_H to 4,
        KeyEvent.KEYCODE_I to 34,
        KeyEvent.KEYCODE_J to 38,
        KeyEvent.KEYCODE_K to 40,
        KeyEvent.KEYCODE_L to 37,
        KeyEvent.KEYCODE_M to 46,
        KeyEvent.KEYCODE_N to 45,
        KeyEvent.KEYCODE_O to 31,
        KeyEvent.KEYCODE_P to 35,
        KeyEvent.KEYCODE_Q to 12,
        KeyEvent.KEYCODE_R to 15,
        KeyEvent.KEYCODE_S to 1,
        KeyEvent.KEYCODE_T to 17,
        KeyEvent.KEYCODE_U to 32,
        KeyEvent.KEYCODE_V to 9,
        KeyEvent.KEYCODE_W to 13,
        KeyEvent.KEYCODE_X to 7,
        KeyEvent.KEYCODE_Y to 16,
        KeyEvent.KEYCODE_Z to 6,
        KeyEvent.KEYCODE_0 to 29,
        KeyEvent.KEYCODE_1 to 18,
        KeyEvent.KEYCODE_2 to 19,
        KeyEvent.KEYCODE_3 to 20,
        KeyEvent.KEYCODE_4 to 21,
        KeyEvent.KEYCODE_5 to 23,
        KeyEvent.KEYCODE_6 to 22,
        KeyEvent.KEYCODE_7 to 26,
        KeyEvent.KEYCODE_8 to 28,
        KeyEvent.KEYCODE_9 to 25,
        KeyEvent.KEYCODE_GRAVE to 50,
        KeyEvent.KEYCODE_MINUS to 27,
        KeyEvent.KEYCODE_EQUALS to 24,
        KeyEvent.KEYCODE_LEFT_BRACKET to 33,
        KeyEvent.KEYCODE_RIGHT_BRACKET to 30,
        KeyEvent.KEYCODE_BACKSLASH to 42,
        KeyEvent.KEYCODE_SEMICOLON to 41,
        KeyEvent.KEYCODE_APOSTROPHE to 39,
        KeyEvent.KEYCODE_COMMA to 43,
        KeyEvent.KEYCODE_PERIOD to 47,
        KeyEvent.KEYCODE_SLASH to 44,
        KeyEvent.KEYCODE_SPACE to 49,
        KeyEvent.KEYCODE_ENTER to 36,
        KeyEvent.KEYCODE_TAB to 48,
        KeyEvent.KEYCODE_DEL to 51,
        KeyEvent.KEYCODE_FORWARD_DEL to 117,
        KeyEvent.KEYCODE_ESCAPE to 53,
        KeyEvent.KEYCODE_CAPS_LOCK to 57,
        KeyEvent.KEYCODE_VOLUME_UP to 72,
        KeyEvent.KEYCODE_VOLUME_DOWN to 73,
        KeyEvent.KEYCODE_VOLUME_MUTE to 74,
        KeyEvent.KEYCODE_DPAD_UP to 126,
        KeyEvent.KEYCODE_DPAD_DOWN to 125,
        KeyEvent.KEYCODE_DPAD_LEFT to 123,
        KeyEvent.KEYCODE_DPAD_RIGHT to 124,
        KeyEvent.KEYCODE_MOVE_HOME to 115,
        KeyEvent.KEYCODE_MOVE_END to 119,
        KeyEvent.KEYCODE_PAGE_UP to 116,
        KeyEvent.KEYCODE_PAGE_DOWN to 121,
        KeyEvent.KEYCODE_SHIFT_LEFT to 56,
        KeyEvent.KEYCODE_SHIFT_RIGHT to 60,
        KeyEvent.KEYCODE_CTRL_LEFT to 59,
        KeyEvent.KEYCODE_CTRL_RIGHT to 62,
        KeyEvent.KEYCODE_ALT_LEFT to 58,
        KeyEvent.KEYCODE_ALT_RIGHT to 61,
        KeyEvent.KEYCODE_META_LEFT to 55,
        KeyEvent.KEYCODE_META_RIGHT to 54,
        KeyEvent.KEYCODE_FUNCTION to 63,
        KeyEvent.KEYCODE_F1 to 122,
        KeyEvent.KEYCODE_F2 to 120,
        KeyEvent.KEYCODE_F3 to 99,
        KeyEvent.KEYCODE_F4 to 118,
        KeyEvent.KEYCODE_F5 to 96,
        KeyEvent.KEYCODE_F6 to 97,
        KeyEvent.KEYCODE_F7 to 98,
        KeyEvent.KEYCODE_F8 to 100,
        KeyEvent.KEYCODE_F9 to 101,
        KeyEvent.KEYCODE_F10 to 109,
        KeyEvent.KEYCODE_F11 to 103,
        KeyEvent.KEYCODE_F12 to 111,
        // Numeric Keypad
        KeyEvent.KEYCODE_NUMPAD_0 to 82,
        KeyEvent.KEYCODE_NUMPAD_1 to 83,
        KeyEvent.KEYCODE_NUMPAD_2 to 84,
        KeyEvent.KEYCODE_NUMPAD_3 to 85,
        KeyEvent.KEYCODE_NUMPAD_4 to 86,
        KeyEvent.KEYCODE_NUMPAD_5 to 87,
        KeyEvent.KEYCODE_NUMPAD_6 to 88,
        KeyEvent.KEYCODE_NUMPAD_7 to 89,
        KeyEvent.KEYCODE_NUMPAD_8 to 91,
        KeyEvent.KEYCODE_NUMPAD_9 to 92,
        KeyEvent.KEYCODE_NUMPAD_DOT to 65,
        KeyEvent.KEYCODE_NUMPAD_MULTIPLY to 67,
        KeyEvent.KEYCODE_NUMPAD_ADD to 69,
        KeyEvent.KEYCODE_NUMPAD_DIVIDE to 75,
        KeyEvent.KEYCODE_NUMPAD_SUBTRACT to 78,
        KeyEvent.KEYCODE_NUMPAD_ENTER to 76,
        KeyEvent.KEYCODE_NUMPAD_EQUALS to 81,
        KeyEvent.KEYCODE_NUM_LOCK to 71
    )

    private val asciiToMacKeyMap = mapOf(
        'a'.code to 0, 'A'.code to 0,
        'b'.code to 11, 'B'.code to 11,
        'c'.code to 8, 'C'.code to 8,
        'd'.code to 2, 'D'.code to 2,
        'e'.code to 14, 'E'.code to 14,
        'f'.code to 3, 'F'.code to 3,
        'g'.code to 5, 'G'.code to 5,
        'h'.code to 4, 'H'.code to 4,
        'i'.code to 34, 'I'.code to 34,
        'j'.code to 38, 'J'.code to 38,
        'k'.code to 40, 'K'.code to 40,
        'l'.code to 37, 'L'.code to 37,
        'm'.code to 46, 'M'.code to 46,
        'n'.code to 45, 'N'.code to 45,
        'o'.code to 31, 'O'.code to 31,
        'p'.code to 35, 'P'.code to 35,
        'q'.code to 12, 'Q'.code to 12,
        'r'.code to 15, 'R'.code to 15,
        's'.code to 1, 'S'.code to 1,
        't'.code to 17, 'T'.code to 17,
        'u'.code to 32, 'U'.code to 32,
        'v'.code to 9, 'V'.code to 9,
        'w'.code to 13, 'W'.code to 13,
        'x'.code to 7, 'X'.code to 7,
        'y'.code to 16, 'Y'.code to 16,
        'z'.code to 6, 'Z'.code to 6,
        '0'.code to 29, '1'.code to 18, '2'.code to 19, '3'.code to 20, '4'.code to 21,
        '5'.code to 23, '6'.code to 22, '7'.code to 26, '8'.code to 28, '9'.code to 25,
        ' '.code to 49, '\n'.code to 36, '\r'.code to 36, '\t'.code to 48,
        8 to 51, // Backspace (ASCII 8) -> Mac Delete (51)
        127 to 51, // ASCII Delete -> Mac Delete (51)
        '`'.code to 50, '~'.code to 50,
        '-'.code to 27, '_'.code to 27,
        '='.code to 24, '+'.code to 24,
        '['.code to 33, '{'.code to 33,
        ']'.code to 30, '}'.code to 30,
        '\\'.code to 42, '|'.code to 42,
        ';'.code to 41, ':'.code to 41,
        '\''.code to 39, '"'.code to 39,
        ','.code to 43, '<'.code to 43,
        '.'.code to 47, '>'.code to 47,
        '/'.code to 44, '?'.code to 44
    )

    fun sendKeyEvent(keyCode: Int, isDown: Boolean, modifiers: Int = 0, isAscii: Boolean = false) {
        val macCode = if (isAscii) {
            asciiToMacKeyMap[keyCode] ?: 0xFFFF
        } else {
            androidToMacKeyMap[keyCode] ?: 0xFFFF
        }
        
        if (macCode == 0xFFFF) return

        val buffer = ByteBuffer.allocate(9)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        // 1. Timestamp (UInt32, 4 Bytes) - Ensure strictly increasing
        var currentTs = System.currentTimeMillis()
        if (currentTs <= lastTimestamp) {
            currentTs = lastTimestamp + 1
        }
        lastTimestamp = currentTs
        
        val timestamp = (currentTs and 0xFFFFFFFFL).toInt()
        buffer.putInt(timestamp)

        // 2. Packet Type (UInt8, 1 Byte) - Fixed 0x02
        buffer.put(0x02.toByte())

        // 3. Action (UInt8, 1 Byte) - 0x00 KeyUp, 0x01 KeyDown
        buffer.put(if (isDown) 0x01.toByte() else 0x00.toByte())

        // 4. Keycode (UInt16, 2 Bytes)
        buffer.putShort(macCode.toShort())

        // 5. Modifiers (UInt8, 1 Byte)
        buffer.put(modifiers.toByte())

        buffer.flip()
        udpManager.sendData(buffer)
    }
}
