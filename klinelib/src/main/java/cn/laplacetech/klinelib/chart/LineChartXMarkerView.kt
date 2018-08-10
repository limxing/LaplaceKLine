package cn.laplacetech.klinelib.chart


import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView

import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DateUtils
import kotlinx.android.synthetic.main.view_mp_real_price_marker.view.*

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class LineChartXMarkerView(context: Context, private val mList: List<HisData>?) : MarkerView(context, R.layout.view_mp_real_price_marker) {

    var dateFormatString = "yyyy-MM-dd"

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val value = e.x.toInt()
        if (mList != null && value < mList.size) {
            tvContent.text = DateUtils.formatDate(mList[value].date, dateFormatString)
        }
        super.refreshContent(e, highlight)
    }
}
