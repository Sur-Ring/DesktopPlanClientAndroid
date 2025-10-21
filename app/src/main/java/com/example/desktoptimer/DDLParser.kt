package com.example.desktoptimer

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters

/**
 * 解析 DDL 字符串并计算剩余时间
 * @param ddl 格式为 "yyyy-MM-dd hh:mm" 的字符串，日期部分可以用 * 代替表示最近的下一个
 * @return 包含解析后的日期时间和剩余时间的字符串，如果解析失败返回错误信息
 */
@RequiresApi(Build.VERSION_CODES.O)
fun parse_DDL(ddl: String?): Int? {
    if(ddl == null){
        return null
    }

    if (ddl.isBlank()) {
        return null
    }

    // 分割日期和时间部分
    val parts = ddl.split(" ")

    val datePart = parts[0]

    try {
        // 解析日期部分
        val now = LocalDateTime.now()
        val target_date = parseDateWithWildcards(datePart, now.toLocalDate())

        // 计算剩余时间
        val target_desc = calculateRemainingTime(now.toLocalDate(), target_date)

        return target_desc
    } catch (e: DateTimeParseException) {
        Log.e("parse_DDL", "日期时间格式错误: ${e.message}")
        return null
    } catch (e: IllegalArgumentException) {
        Log.e("parse_DDL", "日期解析错误: ${e.message}")
        return null
    }
}


/**
 * 解析包含通配符的日期字符串
 * @param dateStr 日期字符串，可能包含 * 通配符
 * @param curDate 当前日期
 * @return 解析后的日期
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun parseDateWithWildcards(dateStr: String, curDate: LocalDate): LocalDate? {
    val parts = dateStr.split("-")
    if (parts.size != 3) {
        throw IllegalArgumentException("日期格式应为 yyyy-MM-dd")
    }

    var year: Int = curDate.year
    var month: Int = curDate.monthValue
    var day: Int = curDate.dayOfMonth

    var case: Int = 0

    // 处理年份通配符
    if(parts[0] == "*"){
        case += 4
    }else{
        try {
            year = parts[0].toInt()
        }catch (e: NumberFormatException){
            return null
        }
    }

    // 处理月份通配符
    if(parts[1] == "*"){
        case += 2
    }else{
        try {
            month = parts[1].toInt()
        }catch (e: NumberFormatException){
            return null
        }
    }

    // 处理日期通配符
    if(parts[2] == "*"){
        case += 1
    }else{
        try {
            day = parts[2].toInt()
        }catch (e: NumberFormatException){
            return null
        }
    }

    if(case==0){
    } // 000. a-b-c
    else if(case==1){
        if(year==curDate.year){ // 如果是今年
            if(curDate.monthValue==month){ // 如果是本月为当天
                day = curDate.dayOfMonth
            }else if(month>curDate.monthValue){ // 未来为1
                day = 1
            }else{ // 过去为当月最后一天
                day = LocalDate.parse("${year.toString().padStart(4,'0')}-${month.toString().padStart(2,'0')}-01").with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
            }
        }else if(year>curDate.year){ // 未来则为1日
            day = 1
        }else{ // 过去为当月最后一天
            day = LocalDate.parse("${year.toString().padStart(4,'0')}-${month.toString().padStart(2,'0')}-01").with(TemporalAdjusters.lastDayOfMonth()).dayOfMonth
        }
    } // 001, a-b-* 如果是今年本月, 为当天, 未来为1, 过去为最后一天
    else if(case==2){
        if(year==curDate.year){ // 如果是今年
            if(curDate.dayOfMonth>day){ // 本月已过为下月, 最高12月
                month = curDate.monthValue+1
                if(month==13)month=12
            }else{ // 本月未过为本月
                month = curDate.monthValue
            }
        }else if(year>curDate.year){ // 未来则为1月
            month = 1
        }else{ // 过去为12月
            month = 12
        }
    } // 010. a-*-c 如果是今年, 本月未过则为当月, 否则为下月, 如果当前为12月则为12月, 未来为1月, 过去为12月
    else if(case==3){
        if(year==curDate.year){ // 如果是今年则为当天
            month = curDate.monthValue
            day = curDate.dayOfMonth
        }else if(year>=curDate.year){ // 未来则为1月1日
            month = 1
            day = 1
        }else{ // 过去为12月31日
            month = 12
            day = 31
        }
    } // 011, a-*-*
    else if(case==4){
        if(LocalDate.parse("${curDate.year}-${month.toString().padStart(2,'0')}-${day.toString().padStart(2,'0')}").isBefore(curDate)) { // 如果本年b月c日已过则为明年
            year = curDate.year+1
        }else{ // 未过为本年
            year = curDate.year
        }
    } // 100, *-b-c
    else if(case==5){
        if(curDate.monthValue==month){ // 处于b月, 当天
            year = curDate.year
            day = curDate.dayOfMonth
        }else if(curDate.monthValue<month){ // 本年c月未到, 为本年c月1日
            year = curDate.year
            day = 1
        }else{ // 本年c月已过, 为明年c月1日
            year = curDate.year+1
            day = 1
        }
    } // 101, *-b-*
    else if(case==6){
        if(curDate.dayOfMonth>day){ // 本月c日已过, 调整到下月
            year = curDate.plusMonths(1).year
            month = curDate.plusMonths(1).monthValue
        }else{ // 本月c日未过
            year = curDate.year
            month = curDate.monthValue
        }
    } // 110, *-*-c
    else {
        year = curDate.year
        month = curDate.monthValue
        day = curDate.dayOfMonth
    } // 111, *-*-*

    // 构建日期字符串
    val formattedDateStr = "${year.toString().padStart(4,'0')}-${month.toString().padStart(2,'0')}-${day.toString().padStart(2,'0')}"

    // 解析日期
    val date = LocalDate.parse(formattedDateStr)

    // 如果解析出的日期在过去，需要调整到未来的日期
    return date
}

/**
 * 计算剩余时间
 * @param now 当前时间
 * @param target 目标时间
 * @return 剩余时间的友好字符串表示
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun calculateRemainingTime(now: LocalDate, target: LocalDate?): Int? {
    if(target == null){
        return null
    }

    val days = ChronoUnit.DAYS.between(now, target).toInt()
    return days
}

// 测试函数
@RequiresApi(Build.VERSION_CODES.O)
fun testParseDdl() {
    val testCases = listOf(
        "2023-12-25 14:30",  // 固定日期
        "*-12-25 14:30",     // 每年的12月25日
        "2023-*-25 14:30",   // 2023年每月的25日
        "2023-12-* 14:30",   // 2023年12月每天
        "*-*-25 14:30",      // 每月的25日
        "*-*-02 14:30",      // 每月的25日
        "*-12-* 14:30",      // 每年12月每天
        "2023-*-* 14:30",    // 2023年每天
        "*-*-* 14:30",       // 每天
        "2025-*-17 14:30",       // 每天
        "2023-02-30 14:30",  // 无效日期
        "invalid-format"     // 无效格式
    )

    testCases.forEach { ddl ->
        println("$ddl -> ${parse_DDL(ddl)}")
    }
}

fun rest_day_to_desc(restDays:Int):String{
    when {
        restDays == 7300000 -> return "无效"
        restDays == 0 -> return "今天"
        restDays == 1 -> return "明天"
        restDays == 2 -> return "后天"
        else -> return "${restDays}天"
    }
}
