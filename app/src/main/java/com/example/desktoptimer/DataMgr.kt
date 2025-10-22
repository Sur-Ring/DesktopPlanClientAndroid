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


class DataManager private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: DataManager? = null
        fun getInstance(context: Context): DataManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        private const val TAG = "DataManager"
        private const val HELLO_PORT = 5051
        private const val DISCOVERY_MESSAGE = "DISCOVER_FLASK_SERVICE"
        private const val TIMEOUT_MS = 3000
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val dataFile = "todo_data.json"
    public var last_tab_list : MutableList<Todo_Tab> = mutableStateListOf<Todo_Tab>()
    public var last_sync_time : String = ""
    public var last_edit_time : String = ""

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

    private var sync_mgr = SyncMgr(this)

    init {
        Log.d(TAG, "DataManager: 初始化")
        loadData()

        // 创建UDP socket
        udp_socket.setBroadcast(true)
        udp_socket.setSoTimeout(TIMEOUT_MS)

        sync_mgr.start_listening()
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
        Log.d(TAG, "开始读取数据")
        val file= File(context.getFilesDir(), dataFile)
        if(!file.canRead()) return
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
    }
}