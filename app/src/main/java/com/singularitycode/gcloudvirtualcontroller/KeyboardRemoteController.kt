package com.singularitycode.gcloudvirtualcontroller

import android.util.Log
import java.nio.ByteBuffer

class KeyboardRemoteController(private val udpManager: UdpSenderManager) {

    /**
     * 協議 1: 普通文字 (Unicode 字串)
     * Byte 0: 0x03
     * Byte 1+: UTF-8 bytes
     */
    fun sendUnicodeText(text: String) {
        if (text.isEmpty()) return
        Log.d("KeyboardRemote", "Sending Unicode Text: $text")
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + textBytes.size)
        buffer.put(0x03.toByte())
        buffer.put(textBytes)
        buffer.flip()
        udpManager.sendData(buffer)
    }

    /**
     * 協議 2: 特殊控制鍵
     * Byte 0: 0x04
     * Byte 1: 動作類型 (1=Del, 2=Enter, 3=Tab, 4=Esc)
     */
    fun sendControlKey(type: Int) {
        val keyName = when(type) {
            1 -> "Backspace/Delete"
            2 -> "Enter/Return"
            3 -> "Tab"
            4 -> "Escape"
            else -> "Unknown($type)"
        }
        Log.d("KeyboardRemote", "Sending Control Key: $keyName")
        val buffer = ByteBuffer.allocate(2)
        buffer.put(0x04.toByte())







        buffer.put(type.toByte())
        buffer.flip()
        udpManager.sendData(buffer)
    }
}


