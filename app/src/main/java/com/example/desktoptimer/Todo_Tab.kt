package com.example.desktoptimer

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.encodeToString
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.Dictionary
import kotlin.collections.plus

class Todo_Tab(json_data:JSONObject?) {
    var name by mutableStateOf("")
    var fold by mutableStateOf(true)
    var entry_list: MutableList<Todo_Entry> = mutableStateListOf<Todo_Entry>()

    init {
        if(json_data != null) {
            name = json_data.get("name") as String
            fold = json_data.get("fold") as Boolean

            entry_list = mutableStateListOf()
            var entry_list_json = json_data.getJSONArray("todo_entry_list")
            for (i in 0 until entry_list_json.length()) {
                entry_list.add(Todo_Entry(entry_list_json.get(i) as JSONObject?))
            }
        }
    }

    fun to_json(): JSONObject{
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("fold", fold)
        var entry_list_json = JSONArray()
        entry_list.forEach { entry_list_json.put(it.to_json()) }
        obj.put("todo_entry_list", entry_list_json)
        return obj
    }
}

@Composable
fun Todo_Tab_View(
    init_tab:Todo_Tab,
    onUpdateTab: (Todo_Tab) -> Unit,
    onRemoveTab: (Todo_Tab) -> Unit,
    onMoveForwardTab: (Todo_Tab) -> Unit,
    onMoveBackwardTab: (Todo_Tab) -> Unit,
) {
    val tab by remember(init_tab) { mutableStateOf(init_tab) }

    // 计算最近项目和到期时间
    val nearestEntry by remember(tab.entry_list) {
        derivedStateOf {
            tab.entry_list.minByOrNull {
                val currentRestDays = parse_DDL(it.ddl) ?: 7300000
                when {
                    currentRestDays < 0 -> 7300000 + currentRestDays
                    else -> currentRestDays
                }
            }
        }
    }

    var nearest_name = remember(nearestEntry) { nearestEntry?.name ?: "" }
    var nearest_ddl = remember(nearestEntry) { rest_day_to_desc(parse_DDL(nearestEntry?.ddl) ?: 7300000) }

    // 计算排序后的条目
    val sortedEntries by remember(tab.entry_list) {
        derivedStateOf {
            tab.entry_list.sortedBy {
                val currentRestDays = parse_DDL(it.ddl) ?: 7300000
                when {
                    currentRestDays < 0 -> 7300000 + currentRestDays
                    else -> currentRestDays
                }
            }
        }
    }

    fun update_entry(entry_data: Todo_Entry){
        Log.d("TODO_Tab", "update_entry:${entry_data}")
        onUpdateTab(tab)
    }

    fun add_entry(){
        Log.d("TODO_Tab", "add_entry start:${tab.entry_list.size}")
        val new_entry = Todo_Entry(null)
        tab.entry_list.add(new_entry)
        Log.d("TODO_Tab", "add_entry end:${tab.entry_list.size}")
        onUpdateTab(tab)
    }

    fun remove_entry(entry_data: Todo_Entry){
        Log.d("TODO_Tab", "remove_entry:${entry_data}")
        tab.entry_list.removeIf { it == entry_data }
        onUpdateTab(tab)
    }

    Column() {
        Row() {
            // 展开/收起箭头按钮
            IconButton(
                onClick = {
                    tab.fold = !tab.fold
                    onUpdateTab(tab)
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (tab.fold) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (tab.fold) "收起" else "展开"
                )
            }

            TextField(
                value = tab.name,                       // 当前文本值
                onValueChange = { newValue ->
                    tab.name = newValue
                    onUpdateTab(tab)
                },      // 文本变化时的回调
                modifier = Modifier.width(100.dp),
            )
            Text(text = nearest_name, modifier = Modifier.width(80.dp))
            Text(text = nearest_ddl, modifier = Modifier.width(60.dp))
            IconButton(
                onClick = { onMoveForwardTab(tab) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowUp, "上移")
            }
            IconButton(
                onClick = { onMoveBackwardTab(tab) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.KeyboardArrowDown, "下移")
            }
            IconButton(
                onClick = { add_entry() },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.Add, "添加")
            }
            IconButton(
                onClick = { onRemoveTab(tab) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(Icons.Filled.Delete, "删除")
            }
        }

        AnimatedVisibility(
            visible = tab.fold,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                if (sortedEntries.isEmpty()) {
                    Text(
                        text = "无待办事项",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                } else {
                    sortedEntries.forEach { entry ->
                        Log.d("TODO_Tab", "create:${entry}")
                        Todo_Entry_View(
                            init_entry = entry,
                            onUpdate = ::update_entry,
                            onRemove = ::remove_entry
                        )
                        Divider(modifier = Modifier.padding(horizontal = 8.dp))
                    }
                }
            }
        }

    }
}