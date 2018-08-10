package cn.laplacetech.android.laplacekline

import android.content.Context
import android.content.res.Configuration
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CoordinatorLayout
import cn.laplacetech.klinelib.model.HisData
import com.alibaba.fastjson.JSON
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.ArrayList
import android.view.WindowManager


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //设置时间轴 时间的format

        klineview.setIsRedDown(true)
        one_minute.setOnClickListener {
            klineview.setDateFormat("HH:mm")
            klineview.initData(get1Day(this, R.raw.one_minute))
        }
        five_minutes.setOnClickListener {
            klineview.setDateFormat("HH:mm")
            klineview.initData(get1Day(this, R.raw.five_minutes))
        }
        fifteen_minutes.setOnClickListener {
            klineview.setDateFormat("HH:mm")
            klineview.initData(get1Day(this, R.raw.fifteen_minutes))
        }
        one_hour.setOnClickListener {
            klineview.setDateFormat("MM-dd HH:mm")
            klineview.initData(get1Day(this, R.raw.one_hour))
        }
        four_hours.setOnClickListener {
            klineview.setDateFormat("MM-dd HH:mm")
            klineview.initData(get1Day(this, R.raw.four_hours))
        }
        twelve_hours.setOnClickListener {
            klineview.setDateFormat("MM-dd HH:mm")
            klineview.initData(get1Day(this, R.raw.twelve_hours))
        }
        one_day.setOnClickListener {
            klineview.setDateFormat("yyyy-MM-dd")
            klineview.initData(get1Day(this, R.raw.oneday))
        }
    }

    fun get1Day(context: Context, oneday: Int): List<HisData> {
        val `is` = context.resources.openRawResource(oneday)
        val writer = StringWriter()
        val buffer = CharArray(1024)
        try {
            val reader = BufferedReader(InputStreamReader(`is`, "UTF-8"))
            var n: Int
            do {
                n = reader.read(buffer)
                if (n > 0)
                    writer.write(buffer, 0, n)
            } while (n != -1)

            `is`.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val json = writer.toString()

        val list = JSON.parseArray(json, Array<Double>::class.java)

        return list.indices
                .map { list[it] }
                .map { HisData(it) }
//        for (i in list.indices) {
//            val m = list[i]
//            val data = HisData(m)
//
//
//            hisData.add(data)
//        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            (appbar.getChildAt(0).layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
            appbar.layoutParams.height = CoordinatorLayout.LayoutParams.MATCH_PARENT
            val lp = window.attributes
//直接对它flags变量操作   LayoutParams.FLAG_FULLSCREEN 表示设置全屏
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_FULLSCREEN
//设置属性
            window.attributes = lp
//意思大致就是  允许窗口扩展到屏幕之外
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        } else {
            (appbar.getChildAt(0).layoutParams as AppBarLayout.LayoutParams).scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
            appbar.layoutParams.height = CoordinatorLayout.LayoutParams.WRAP_CONTENT
            val lp2 = window.attributes
//LayoutParams.FLAG_FULLSCREEN 强制屏幕状态条栏弹出
            lp2.flags = lp2.flags and WindowManager.LayoutParams.FLAG_FULLSCREEN.inv()
//设置属性
            window.attributes = lp2
//不允许窗口扩展到屏幕之外  clear掉了
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        }

    }

}
