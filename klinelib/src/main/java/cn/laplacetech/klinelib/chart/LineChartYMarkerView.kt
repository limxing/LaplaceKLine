package cn.laplacetech.klinelib.chart


import android.content.Context
import android.widget.TextView

import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.util.DoubleUtil
import kotlinx.android.synthetic.main.view_mp_real_price_marker.view.*

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class LineChartYMarkerView(context: Context) : MarkerView(context, R.layout.view_mp_real_price_marker) {


    override fun refreshContent(e: Entry, highlight: Highlight) {
        val value = e.y
        tvContent.text = DoubleUtil.amountConversion(value.toDouble(),false)
        super.refreshContent(e, highlight)
    }

}
