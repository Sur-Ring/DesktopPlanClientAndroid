package com.example.desktoptimer

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dataManager: DataManager = DataManager.getInstance(application)
    private val _tabList = mutableStateListOf<Todo_Tab>()
    val tabList: MutableList<Todo_Tab> get() = _tabList

    init {
        Log.d("MainViewModel", "MainViewModel: 初始化")
        _tabList.addAll(dataManager.get_tab_list())
    }

    fun updateData() {
        dataManager.update_tab_list(_tabList)
    }

    fun addTab(tab: Todo_Tab) {
        _tabList.add(tab)
        updateData()
    }

    fun removeTab(tab: Todo_Tab) {
        _tabList.remove(tab)
        updateData()
    }
}