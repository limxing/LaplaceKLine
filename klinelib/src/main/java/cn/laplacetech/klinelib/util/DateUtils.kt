package cn.laplacetech.klinelib.util

import java.text.SimpleDateFormat
import java.util.Locale
/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
object DateUtils {

    fun formatDate(time: Long, format: String): String {
        val dateFormat2 = SimpleDateFormat(format, Locale.getDefault())
        return dateFormat2.format(time)
    }


    fun formatDate(time: Long): String {
        val dateFormat2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat2.format(time)
    }


    fun formatDateTime(date: Long): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }


    fun formatTime(millis: Long): String {
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(millis)
    }


}
