package com.example.desktoptimer

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.time.LocalDateTime

class DataManager(private val context: Context) {
    private val dataFile = "todo_data.json"
    private var last_tab_list : MutableList<Todo_Tab> = mutableStateListOf<Todo_Tab>()
    private var last_sync_time : String = ""
    private var last_edit_time : String = ""

    private val server_finder = ServerFinder()

    init {
        loadData()
//        server_finder.startDiscovery()
    }

    fun syncData(){

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
        saveData()
        update_widget()
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
        val f = File(context.getFilesDir(), dataFile)
        val app_data = JSONObject()
        app_data.put("sync_time", last_sync_time)
        app_data.put("edit_time", last_edit_time)
        var tab_list_json = JSONArray()
        last_tab_list.forEach { tab_list_json.put(it.to_json()) }
        app_data.put("tab_list", tab_list_json)
        f.writeText(app_data.toString())
    }
}