package com.example.desktoptimer

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import java.time.LocalDateTime

/**
 * Implementation of App Widget functionality.
 */
class DeskTimer : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
//        // 接收到广播之后回调此方法。
//        context?.also { usableContext ->
//            if (!SpUtils.isInit()) {
//                SpUtils.init(usableContext)
//            }
//            var changed = false
//            // 实测全局变量在每次接受广播时都会重置。
//            // 简单通过SharedPreference从本地存取，确保可以使用最新的数据。
//            var currentTotalWaterCount = SpUtils.getInt("currentTotalWaterCount", 0)
//            when (intent?.action) {
//                ACTION_INCREASE -> {
//                    changed = true
//                    currentTotalWaterCount++
//                }
//
//                ACTION_DECREASE -> {
//                    if (currentTotalWaterCount > 0) {
//                        changed = true
//                        currentTotalWaterCount--
//                    }
//                }
//            }
//            if (changed) {
//                SpUtils.put("currentTotalWaterCount", currentTotalWaterCount)
//                getWidgetManager(usableContext).run {
//                    updateWidget(usableContext, this, getAppWidgetIds(ComponentName(usableContext, ExampleDesktopWidgetProvider::class.java)))
//                }
//            }
//        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        // 添加一个静态方法来手动更新小组件
        fun updateWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, DeskTimer::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {

    val views = RemoteViews(context.packageName, R.layout.desk_timer)
    val contentContainer = R.id.content_container
    views.removeAllViews(contentContainer)

    val dataManager = DataManager.getInstance(context)
    val tab_list = dataManager.get_tab_list()
    Log.d("updateAppWidget", "更新小组件: ${tab_list.size}")

    tab_list.forEachIndexed { index, tab ->
        // 只显示前5个tab，避免小组件过大
        if (index < 5) {
            addTabRow(context, views, contentContainer, tab)
        }
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

fun addTabRow(
    context: Context,
    views: RemoteViews,
    containerId: Int,
    tab: Todo_Tab
) {
    // 创建行视图
    val rowViews = RemoteViews(context.packageName, R.layout.widget_tab_row)

    // 设置项目名称
    rowViews.setTextViewText(R.id.tab_name, tab.name)

    // 找到最近的待办事项
    val nearestEntry = findNearestEntry(tab)

    // 设置最近到期子项目
    val entryName = nearestEntry?.name ?: "无待办事项"
    rowViews.setTextViewText(R.id.nearest_entry, entryName)

    // 设置到期时间
    val dueText = if (nearestEntry != null) {
        rest_day_to_desc(parse_DDL(nearestEntry.ddl) ?: 7300000)
//        formatDueTime(nearestEntry.ddl)
    } else {
        ""
    }
    rowViews.setTextViewText(R.id.due_time, dueText)
    Log.d("TAG", "addTabRow: 添加 ${tab.name}, ${nearestEntry?.name}, ${nearestEntry?.ddl}")
    // 设置点击意图（打开应用）
//    setTabClickIntent(context, rowViews, tab.id)

    // 添加到容器
    views.addView(containerId, rowViews)
}

fun findNearestEntry(tab: Todo_Tab): Todo_Entry? {
    return tab.entry_list.minByOrNull { entry ->
        val restDays = parse_DDL(entry.ddl) ?: Int.MAX_VALUE
        when {
            restDays < 0 -> Int.MAX_VALUE + restDays  // 过期事项排在后面
            else -> restDays
        }
    }
}