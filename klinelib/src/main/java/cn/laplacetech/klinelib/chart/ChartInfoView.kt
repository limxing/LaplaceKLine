package cn.laplacetech.klinelib.chart

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

import com.github.mikephil.charting.charts.Chart
import cn.laplacetech.klinelib.model.HisData

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

abstract class ChartInfoView : LinearLayout {


    //    var mLineCharts: Array<Chart<*>>? = null
    protected var mRunnable: Runnable = Runnable {
        visibility = View.GONE
        if (mLineCharts != null) {
            for (chart in mLineCharts!!) {
                chart.highlightValue(null)
            }
        }
    }

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    abstract fun setData(lastClose: Double, data: HisData)

    private var mLineCharts: Array<out Chart<*>>? = null

    fun setChart(vararg chart: Chart<*>) {
        mLineCharts = chart
    }
}
