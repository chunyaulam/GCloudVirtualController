package com.singularitycode.gcloudvirtualcontroller

import java.nio.ByteBuffer
import java.nio.ByteOrder

class MouseRemoteController(private val udpManager: UdpSenderManager) {

    private var currentButtonsBitmask: Byte = 0x00

    fun mouseMove(deltaX: Float, deltaY: Float) {
        val buffer = buildPacket(deltaX = deltaX, deltaY = deltaY, buttonsBitmask = currentButtonsBitmask)
        udpManager.sendData(buffer)
    }

    fun leftClick(isPressed: Boolean) {
        updateBitmask(0x01, isPressed)
        val buffer = buildPacket(buttonsBitmask = currentButtonsBitmask)
        udpManager.sendData(buffer)
    }

    fun rightClick(isPressed: Boolean) {
        updateBitmask(0x02, isPressed)
        val buffer = buildPacket(buttonsBitmask = currentButtonsBitmask)
        udpManager.sendData(buffer)
    }

    fun middleClick(isPressed: Boolean) {
        updateBitmask(0x04, isPressed)
        val buffer = buildPacket(buttonsBitmask = currentButtonsBitmask)
        udpManager.sendData(buffer)
    }

    private fun updateBitmask(mask: Int, isPressed: Boolean) {
        currentButtonsBitmask = if (isPressed) {
            (currentButtonsBitmask.toInt() or mask).toByte()
        } else {
            (currentButtonsBitmask.toInt() and mask.inv()).toByte()
        }
    }

    fun scroll(deltaY: Byte, deltaX: Byte = 0) {
        val buffer = buildPacket(scrollWheel = deltaY, hScrollWheel = deltaX)
        udpManager.sendData(buffer)
    }

    private fun buildPacket(
        deltaX: Float = 0f,
        deltaY: Float = 0f,
        buttonsBitmask: Byte = 0,
        scrollWheel: Byte = 0,
        hScrollWheel: Byte = 0
    ): ByteBuffer {
        return ByteBuffer.allocate(16).apply {
            order(ByteOrder.LITTLE_ENDIAN)

            val timestamp = System.currentTimeMillis().toInt()
            val packetType: Byte = 0x01 // Mouse Event

            putInt(timestamp)
            put(packetType)
            put(buttonsBitmask)
            putFloat(deltaX)
            putFloat(deltaY)
            put(scrollWheel)
            put(hScrollWheel)

            flip()
        }
    }

    fun release() {
        // No resources to release locally
    }
}
