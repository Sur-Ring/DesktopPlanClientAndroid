package com.example.desktoptimer

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources.createFactory
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier


class SyncMgr(private var data_mgr: DataManager) {
    companion object {
        private const val TAG = "SyncMgr"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private var eventSource: EventSource? = null

    private var server_ip : String? = null
    private var server_port : Int = 0
    private var server_finder:ServerFinder = ServerFinder({_server_ip,_server_port ->
        Log.i("SyncMgr", "已获取服务器ip与端口")
        server_ip = _server_ip
        server_port = _server_port
        start_listening()
    })
    private val http_client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .hostnameVerifier(HostnameVerifier { hostname, session -> true })
            .build()
    }

    init {
        Log.d(TAG, "SyncMgr: 初始化")
        ServerFinder({_server_ip,_server_port ->
            Log.i("SyncMgr", "已获取服务器ip与端口")
            server_ip = _server_ip
            server_port = _server_port
            start_listening()
        })
    }

    fun start_listening() {
        Log.i("SyncMgr", "开始监听SSE")

        if(server_ip == null){
            server_finder.discover_server()
            return
        }

        var url = "http://"+server_ip+":"+server_port+"/sync_time"
        Log.i("SyncMgr", "尝试链接到${url}")

        val request: Request = Request.Builder()
            .url(url=url)
            .header("Accept", "text/event-stream")
            .build()

        val factory = createFactory(http_client)
        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.i("SyncMgr", "SSE连接已打开 - 响应码: ${response.code}")
                Log.i("SyncMgr", "响应头: ${response.headers}")

                // 检查响应内容类型
                val contentType = response.header("Content-Type", "")
                Log.i("SyncMgr", "Content-Type: $contentType")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                Log.i("SyncMgr", "收到事件 - 类型: " + type + ", 数据: " + data)

                if(data!=data_mgr.last_sync_time){
                    get_data()
                }else if(data_mgr.last_sync_time != data_mgr.last_edit_time){
                    put_data()
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.i("SyncMgr", "SSE连接已关闭")
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                val errorMsg = if (t != null) t.message else "未知错误"
                Log.e("SyncMgr", "SSE连接失败: " + errorMsg)
                server_ip = null;

                // 可以在这里实现自动重连
                start_listening();
            }
        })
    }

    fun get_data(){
        // 发送GET请求
        val url = "http://$server_ip:$server_port/api/data/pull"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        http_client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("SyncMgr", "网络错误: ${e.message}")
                server_ip = null
                start_listening();
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        Log.d("TAG", "收到回复:${responseBody}")
                        val jsonObject = JSONObject(responseBody)
                        val syncTime = jsonObject.optString("sync_time", "")
                        // 将服务器的数据作为自己的数据
                        data_mgr.last_sync_time = syncTime
                        data_mgr.last_edit_time = data_mgr.last_sync_time
                        data_mgr.last_tab_list.clear()
                        var tab_list_json = jsonObject.getJSONArray("tab_list")
                        for (i in 0 until tab_list_json.length()) {
                            data_mgr.last_tab_list.add(Todo_Tab(tab_list_json.get(i) as JSONObject?))
                        }
                        data_mgr.saveData()
                    } else {
                        Log.d("TAG", "服务器错误: ${response.code}")
                    }
                }
            }
        })
    }

    fun put_data(){
        val app_data = JSONObject()
        app_data.put("sync_time", data_mgr.last_sync_time)
        var tab_list_json = JSONArray()
        data_mgr.last_tab_list.forEach { tab_list_json.put(it.to_json()) }
        app_data.put("tab_list", tab_list_json)

        // 发送PUT请求
        val url = "http://$server_ip:$server_port/api/data/push"
        val requestBody = app_data.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .build()

        http_client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("SyncMgr", "网络错误: ${e.message}")
                server_ip = null
                start_listening();
            }
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        Log.d("TAG", "收到回复:${responseBody}")
                        val jsonObject = JSONObject(responseBody)
                        val syncTime = jsonObject.optString("sync_time", "")
                        // 同步成功, 更新同步时间
                        data_mgr.last_sync_time = syncTime
                        data_mgr.last_edit_time = data_mgr.last_sync_time
                    } else {
                        Log.d("TAG", "服务器错误: ${response.code}")
                    }
                }
            }
        })
    }
}