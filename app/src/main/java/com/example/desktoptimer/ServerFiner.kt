package com.example.desktoptimer

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class ServerFinder {
    private var udpSocket: DatagramSocket = DatagramSocket()
    private var discoveredServerIp: String? = null
    private var discoveredServerPort: Int = 0

    init {
        // 创建UDP socket
        udpSocket.setBroadcast(true)
        udpSocket.setSoTimeout(TIMEOUT_MS)
    }

    fun startDiscovery() {
        DiscoveryThread().start()
    }

    private inner class DiscoveryThread : Thread() {
        override fun run() {
            // 发送广播消息
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val sendData = DISCOVERY_MESSAGE.toByteArray()
            val sendPacket = DatagramPacket(
                sendData, sendData.size, broadcastAddress, DISCOVERY_PORT
            )

            udpSocket.send(sendPacket)
            Log.d(TAG, "广播发现消息已发送")

            // 监听响应
            val receiveBuffer = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            try {
                udpSocket.receive(receivePacket)
            } catch (e: SocketTimeoutException) {
                Log.d(TAG, "超时未收到响应")
                return
            }

            val response = String(receivePacket.getData(), 0, receivePacket.getLength())
            val senderIp = receivePacket.getAddress().getHostAddress()
            Log.d(TAG, "收到响应: " + response + " 来自: " + senderIp)

            if (response.startsWith("FLASK_SERVICE:")) {
                val parts: Array<String?> =
                    response.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()

                if (parts.size < 3) {
                    Log.d(TAG, "回复格式错误")
                }

                val servicePort = parts[2]!!.toInt()
                if (senderIp == null) {
                    Log.d(TAG, "senderIp为空")
                }
                discoveredServerIp = senderIp
                discoveredServerPort = servicePort
                Log.d("TAG", "发现服务器: $senderIp:$servicePort")
            }
        }
    }

    companion object {
        private const val TAG = "NetworkDiscovery"
        private const val DISCOVERY_PORT = 5051
        private const val DISCOVERY_MESSAGE = "DISCOVER_FLASK_SERVICE"
        private const val TIMEOUT_MS = 3000
    }
}