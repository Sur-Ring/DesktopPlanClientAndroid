package com.example.desktoptimer

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class ServerFinder(private var on_found_server : (String, Int)->Unit) {
    companion object {
        private const val TAG = "ServerFinder"
        private const val HELLO_PORT = 5051
        private const val DISCOVERY_MESSAGE = "DISCOVER_FLASK_SERVICE"
        private const val TIMEOUT_MS = 3000
    }
    private var udp_socket: DatagramSocket = DatagramSocket()
    init {
        Log.d("ServerFinder", "ServerFinder:初始化")
        // 创建UDP socket
        udp_socket.setBroadcast(true)
        udp_socket.setSoTimeout(TIMEOUT_MS)
    }

    fun discover_server() {
        DiscoveryThread().start()
    }

    private inner class DiscoveryThread : Thread() {
        override fun run() {
            Log.i("ServerFinder", "开始寻找服务器")
//            var udp_socket: DatagramSocket = DatagramSocket()
//            udp_socket.setBroadcast(true)
//            udp_socket.setSoTimeout(TIMEOUT_MS)
            while (true){
                // 发送广播消息
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val sendData = DISCOVERY_MESSAGE.toByteArray()
                val sendPacket = DatagramPacket(
                    sendData, sendData.size, broadcastAddress, HELLO_PORT
                )

                udp_socket.send(sendPacket)
                Log.d(TAG, "广播发现消息已发送")

                // 监听响应
                val receiveBuffer = ByteArray(1024)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

                try {
                    udp_socket.receive(receivePacket)
                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "超时未收到响应")
                    continue
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
                    Log.d("TAG", "发现服务器: $senderIp:$servicePort")
                    on_found_server(senderIp, servicePort)
//                    udp_socket.close()
                    return
                }
            }
        }
    }
}