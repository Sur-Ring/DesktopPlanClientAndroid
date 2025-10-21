package com.example.desktoptimer

import android.R
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.desktoptimer.ui.theme.DesktopTimerTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row

import androidx.compose.ui.unit.dp

import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Collections
import kotlin.collections.plus
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    private lateinit var dataManager: DataManager
    private var tab_list : MutableList<Todo_Tab> = mutableStateListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 加载存储的数据
        dataManager = DataManager(this)
//        tab_list = dataManager.get_tab_list()
        tab_list.addAll(dataManager.get_tab_list())

        fun update_data(){
            dataManager.update_tab_list(tab_list)
        }

        // 创建UI
        setContent {
            DesktopTimerTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        TODO_Page(
                            tab_list,
                            ::update_data,
                        )

                        OutlinedButton(onClick = {
//            testParseDdl()
                            DeskTimer.updateWidgets(this@MainActivity)
                        }) {
                            Text("测试按钮")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TODO_Page(
    tab_list: MutableList<Todo_Tab>,
    onUpdateData: () -> Unit,
    ) {
    fun move_forward_tab(tab_data: Todo_Tab){
        Log.d("TODO_Page", "move_forward_tab id:${tab_data}")
        val currentIndex = tab_list.indexOfFirst { it == tab_data }
        // 如果没找到或者已经在最前面，返回
        if (currentIndex == -1 || currentIndex == 0) {
            return
        }

        // 交换位置：将当前元素与前一个元素交换
        Collections.swap(tab_list, currentIndex, currentIndex - 1)
        onUpdateData()
    }

    fun move_backward_tab(tab_data: Todo_Tab){
        Log.d("TODO_Page", "move_backward_tab id:${tab_data}")
        val currentIndex = tab_list.indexOfFirst { it == tab_data }
        // 如果没找到或者已经在最后面，返回
        if (currentIndex == -1 || currentIndex == tab_list.size - 1) {
            return
        }

        // 交换位置：将当前元素与前一个元素交换
        Collections.swap(tab_list, currentIndex, currentIndex + 1)
        onUpdateData()
    }

    fun update_tab(tab_data: Todo_Tab){
        Log.d("TODO_Page", "update_tab:${tab_data}")
        onUpdateData()
    }

    fun add_tab(){
        Log.d("TODO_Page", "add_tab")
        var new_tab = Todo_Tab(null)
        tab_list.add(new_tab)
        onUpdateData()
    }

    fun remove_tab(tab_data: Todo_Tab){
        Log.d("TODO_Page", "remove_tab:${tab_data}")
        tab_list.removeIf { it == tab_data }
        onUpdateData()
    }

    Column() {
        Row() {
            OutlinedButton(onClick = {}) {
                Text("已同步")
            }

            OutlinedButton(onClick = ::add_tab) {
                Text("+")
            }
        }

        Column() {
            tab_list.forEach { tab ->
                Todo_Tab_View(
                    init_tab = tab,
                    onUpdateTab = ::update_tab,
                    onRemoveTab = ::remove_tab,
                    onMoveForwardTab = ::move_forward_tab,
                    onMoveBackwardTab = ::move_backward_tab,
                )
            }
        }
    }
}