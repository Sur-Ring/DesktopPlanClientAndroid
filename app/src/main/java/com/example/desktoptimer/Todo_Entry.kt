package com.example.desktoptimer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.Dictionary

class Todo_Entry(json_data:JSONObject?) {
    var name by mutableStateOf("")
    var ddl by mutableStateOf("")
    var rest_days by mutableStateOf(0)

    init {
        if(json_data != null){
            name = json_data.get("name") as String
            ddl = json_data.get("ddl") as String
        }
    }

    fun to_json(): JSONObject{
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("ddl", ddl)
        return obj
    }
}

@Composable
fun Todo_Entry_View(
    init_entry:Todo_Entry,
    onUpdate: (Todo_Entry) -> Unit,
    onRemove: (Todo_Entry) -> Unit
) {
    val entry by remember(init_entry) { mutableStateOf(init_entry) }

    val restDays = remember(entry.ddl) {
        derivedStateOf {
            parse_DDL(entry.ddl) ?: 7300000
        }
    }.value

    val remainingDays = remember(restDays) {
        derivedStateOf {
            rest_day_to_desc(restDays)
        }
    }.value

    Row() {
        TextField(
            value = entry.name,                       // 当前文本值
            onValueChange = { newValue ->
                entry.name = newValue
                onUpdate(entry)
            },      // 文本变化时的回调
            modifier = Modifier.weight(1f),
        )

        TextField(
            value = entry.ddl,                       // 当前文本值
            onValueChange = { newValue ->
                entry.ddl = newValue
                onUpdate(entry)
            },      // 文本变化时的回调
            modifier = Modifier.width(140.dp),
        )

        Text(text = remainingDays, modifier = Modifier.width(60.dp))

        OutlinedButton(onClick = { onRemove(entry) }) {
            Text("-")
        }
    }
}