package cn.laplacetech.android.laplacekline

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import cn.laplacetech.klinelib.model.HisData
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSON.parseObject
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.StringWriter
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        klineview.setDateFormat("yyyy-MM-dd")//设置时间轴 时间的format
        klineview.showVolume()

        klineview.initData(get1Day(this))
    }

    fun get1Day(context: Context): List<HisData> {
        val `is` = context.resources.openRawResource(R.raw.oneday)
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

        val hisData = ArrayList<HisData>()
        for (i in list.indices) {
            val m = list[i]
            val data = HisData(m)


            hisData.add(data)
        }
        return hisData
    }

}
