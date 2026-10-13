package com.singularitycode.gcloudvirtualcontroller

import android.os.SystemClock
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.PortUnreachableException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class UdpSenderManager(private val macIp: String, private val port: Int) {

    private var socket: DatagramSocket? = null
    private val sendExecutor = Executors.newSingleThreadExecutor()
    private val listenExecutor = Executors.newSingleThreadExecutor()
    private var address: InetAddress? = null
    private val isClosed = AtomicBoolean(false)
    private val isDisconnectedNotified = AtomicBoolean(false)

    var onDisconnected: ((String) -> Unit)? = null

    init {
        sendExecutor.execute {
            try {
                val sock = DatagramSocket()
                sock.soTimeout = 1000
                socket = sock
                address = InetAddress.getByName(macIp)
                startHeartbeatAndReceiver(sock)
            } catch (e: Exception) {
                notifyDisconnected("Unable to connect to $macIp:$port (${e.localizedMessage})")
            }
        }
    }

    private fun startHeartbeatAndReceiver(sock: DatagramSocket) {
        listenExecutor.execute {
            val receiveBuffer = ByteArray(1024)
            val packet = DatagramPacket(receiveBuffer, receiveBuffer.size)
            var lastPingSentTime = 0L
            var lastResponseTime = SystemClock.elapsedRealtime()

            while (!isClosed.get() && !sock.isClosed) {
                val now = SystemClock.elapsedRealtime()

                // Send Ping packet (Type 0x00) every 2 seconds
                if (now - lastPingSentTime >= 2000L) {
                    lastPingSentTime = now
                    sendPing(sock)
                }

                try {
                    sock.receive(packet)
                    // Received response or message from server
                    lastResponseTime = SystemClock.elapsedRealtime()

                    if (packet.length > 0) {
                        val firstByte = receiveBuffer[0].toInt() and 0xFF
                        if (firstByte == 0xFF) { // 0xFF = Server Disconnect Signal
                            notifyDisconnected("Server sent disconnect signal.")
                            break
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    // Timeout on receive is expected; check if we haven't received anything in 6 seconds
                    if (now - lastResponseTime > 6000L) {
                        notifyDisconnected("Server connection timed out.")
                        break
                    }
                } catch (e: PortUnreachableException) {
                    notifyDisconnected("Server port $port is unreachable.")
                    break
                } catch (e: SocketException) {
                    if (!isClosed.get()) {
                        notifyDisconnected("Network connection reset.")
                    }
                    break
                } catch (e: Exception) {
                    if (!isClosed.get()) {
                        notifyDisconnected("Connection error: ${e.localizedMessage}")
                    }
                    break
                }
            }
        }
    }

    private fun sendPing(sock: DatagramSocket) {
        try {
            val target = address ?: return
            val pingBuffer = ByteBuffer.allocate(5).apply {
                putInt(System.currentTimeMillis().toInt())
                put(0x00.toByte()) // Ping packet type
                flip()
            }
            val bytes = pingBuffer.array()
            val packet = DatagramPacket(bytes, bytes.size, target, port)
            sock.send(packet)
        } catch (e: PortUnreachableException) {
            notifyDisconnected("Server port $port is unreachable.")
        } catch (e: SocketException) {
            if (!isClosed.get()) {
                notifyDisconnected("Server connection lost.")
            }
        } catch (e: Exception) {
            // Ignore minor ping send errors
        }
    }

    fun sendData(udpData: ByteBuffer) {
        if (isClosed.get()) return

        val length = udpData.remaining()
        val byteData = ByteArray(length)
        udpData.mark()
        udpData.get(byteData)
        udpData.reset()

        sendExecutor.execute {
            try {
                val targetAddress = address
                val currentSocket = socket

                if (currentSocket != null && targetAddress != null && !isClosed.get() && !currentSocket.isClosed) {
                    val packet = DatagramPacket(byteData, byteData.size, targetAddress, port)
                    currentSocket.send(packet)
                }
            } catch (e: PortUnreachableException) {
                notifyDisconnected("Server port $port is unreachable.")
            } catch (e: SocketException) {
                if (!isClosed.get()) {
                    notifyDisconnected("Server connection lost.")
                }
            } catch (e: Exception) {
                // Ignore transient send errors
            }
        }
    }

    private fun notifyDisconnected(reason: String) {
        if (isClosed.get()) return
        if (isDisconnectedNotified.compareAndSet(false, true)) {
            onDisconnected?.invoke(reason)
        }
    }

    fun close() {
        if (isClosed.compareAndSet(false, true)) {
            sendExecutor.execute {
                try {
                    socket?.close()
                    socket = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            sendExecutor.shutdown()
            listenExecutor.shutdown()
        }
    }
}
