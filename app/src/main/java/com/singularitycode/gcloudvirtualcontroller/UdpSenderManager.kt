package com.singularitycode.gcloudvirtualcontroller

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class UdpSenderManager(private val macIp: String, private val port: Int) {

    private var socket: DatagramSocket? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var address: InetAddress? = null

    init {
        executor.execute {
            try {
                socket = DatagramSocket()
                address = InetAddress.getByName(macIp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendData(udpData: ByteBuffer) {
        val length = udpData.remaining()
        val byteData = ByteArray(length)
        udpData.mark()
        udpData.get(byteData)
        udpData.reset()

        executor.execute {
            try {
                val targetAddress = address
                val currentSocket = socket

                if (currentSocket != null && targetAddress != null) {
                    val packet = DatagramPacket(byteData, byteData.size, targetAddress, port)
                    currentSocket.send(packet)
                }
            } catch (e: Exception) {
                // Ignore errors in background
            }
        }
    }

    fun close() {
        executor.execute {
            try {
                socket?.close()
                socket = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        executor.shutdown()
    }
}
