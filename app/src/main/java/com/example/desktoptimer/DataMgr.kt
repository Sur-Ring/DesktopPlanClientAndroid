package com.example.desktoptimer

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier


class DataManager(private val context: Context) {
    companion object {
        private const val TAG = "DataManager"
        private const val HELLO_PORT = 5051
        private const val DISCOVERY_MESSAGE = "DISCOVER_FLASK_SERVICE"
        private const val TIMEOUT_MS = 3000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val dataFile = "todo_data.json"
    private var last_tab_list : MutableList<Todo_Tab> = mutableStateListOf<Todo_Tab>()
    private var last_sync_time : String = ""
    private var last_edit_time : String = ""

    private var udp_socket: DatagramSocket = DatagramSocket()
    public var server_ip: String? = null
    public var server_port: Int = 0
    private val http_client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .hostnameVerifier(HostnameVerifier { hostname, session -> true })
            .build()
    }

    init {
        loadData()

        // 创建UDP socket
        udp_socket.setBroadcast(true)
        udp_socket.setSoTimeout(TIMEOUT_MS)

//        if (last_sync_time != last_edit_time) {
//            Log.d(TAG, "初始化触发同步数据")
//            syncData()
//        }
    }

    fun update_widget(){
        DeskTimer.updateWidgets(context)
    }

    fun get_tab_list(): MutableList<Todo_Tab> {
        return last_tab_list
    }

    fun update_tab_list(new_tab_list: MutableList<Todo_Tab>):Unit {
        Log.d("TAG", "update_tab_list: ${new_tab_list}")
        last_tab_list = new_tab_list
        val date_time_formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        last_edit_time = LocalDateTime.now().format(date_time_formatter)
        saveData()
    }

    fun loadData() {
        val file= File(context.getFilesDir(), dataFile)
        val app_data = JSONObject(file.readText())
        if(app_data.has("sync_time")){
            last_sync_time = app_data.getString("sync_time")
        }
        if(app_data.has("edit_time")){
            last_edit_time = app_data.getString("edit_time")
        }
        if(app_data.has("tab_list")){
            last_tab_list = mutableListOf()
            var tab_list_json = app_data.getJSONArray("tab_list")
            for (i in 0 until tab_list_json.length()) {
                last_tab_list.add(Todo_Tab(tab_list_json.get(i) as JSONObject?))
            }
        }
    }

    fun saveData() {
        Log.d(TAG, "开始保存数据")
        val f = File(context.getFilesDir(), dataFile)
        val app_data = JSONObject()
        app_data.put("sync_time", last_sync_time)
        app_data.put("edit_time", last_edit_time)
        var tab_list_json = JSONArray()
        last_tab_list.forEach { tab_list_json.put(it.to_json()) }
        app_data.put("tab_list", tab_list_json)
        f.writeText(app_data.toString())
        update_widget()

        if (last_sync_time != last_edit_time) {
            Log.d(TAG, "保存触发同步数据")
            syncData();
        }
    }

    fun syncData(){
        Log.d(TAG, "开始同步数据")
        if (last_sync_time == last_edit_time) return

        if (server_ip == null) {
            Log.d(TAG, "syncData: server_ip:${server_ip}")
            discover_server()
            // 应该做个回调
            return;
        }

        // 打包数据
        val app_data = JSONObject()
        app_data.put("sync_time", last_sync_time)
        var tab_list_json = JSONArray()
        last_tab_list.forEach { tab_list_json.put(it.to_json()) }
        app_data.put("tab_list", tab_list_json)

        // 发送POST请求
        val url = "http://$server_ip:$server_port/api/data/push"
        val requestBody = app_data.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        http_client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "网络错误: ${e.message}")
                server_ip = null
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        Log.d(TAG, "收到回复:${responseBody}")
                        val jsonObject = JSONObject(responseBody)
                        val result = jsonObject.getInt("result")
                        val syncTime = jsonObject.optString("sync_time", "")

                        if (result == -1){
                            // 如果同步失败, 那么直接将服务器的数据作为自己的数据
                            last_sync_time = syncTime
                            last_edit_time = last_sync_time
                            last_tab_list.clear()
                            var tab_list_json = jsonObject.getJSONArray("tab_list")
                            for (i in 0 until tab_list_json.length()) {
                                last_tab_list.add(Todo_Tab(tab_list_json.get(i) as JSONObject?))
                            }
                            saveData()
                        }else if (result == 0){
                            // 同步成功, 更新同步时间
                            last_sync_time = syncTime
                            last_edit_time = last_sync_time
                            saveData();
                        }
                    } else {
                        Log.d(TAG, "服务器错误: ${response.code}")
                    }
                }
            }
        })
    }

    fun discover_server() {
        if(server_ip != null) return
        DiscoveryThread().start()
    }

    private inner class DiscoveryThread : Thread() {
        override fun run() {
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
                server_ip = senderIp
                server_port = servicePort
                Log.d("TAG", "发现服务器: $senderIp:$servicePort")
                Log.d(TAG, "发现触发同步数据")
                syncData()
            }
        }
    }
}