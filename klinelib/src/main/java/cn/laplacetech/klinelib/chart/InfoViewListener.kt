package cn.laplacetech.klinelib.chart

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DisplayUtils
import cn.laplacetech.klinelib.view.KLineMarkView

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

class InfoViewListener : OnChartValueSelectedListener {

    private var mList: List<HisData>? = null
    private var mLastClose: Double = 0.toDouble()
    private var mInfoView: ChartInfoView? = null
    private var mWidth: Int = 0

    /**
     * if otherChart not empty, highlight will disappear after 3 second
     */
//    private val mOtherChart: Array<Chart<*>>?

    constructor(context: Context, lastClose: Double, list: List<HisData>, infoView: ChartInfoView) {
        mWidth = DisplayUtils.getWidthHeight(context)[0]
        mLastClose = lastClose
        mList = list
        mInfoView = infoView
    }

    private var mOtherChart: Array<out Chart<*>>? = null

    constructor(context: Context, lastClose: Double, list: List<HisData>, infoView: ChartInfoView, vararg otherChart: Chart<*>) {
        mWidth = DisplayUtils.getWidthHeight(context)[0]
        mLastClose = lastClose
        mList = list
        mInfoView = infoView
        mOtherChart = otherChart
    }

    private var mMarkView: KLineMarkView? = null

    private var mKlineView: KLineView? = null

    constructor(klineView:KLineView, context: Context, lastClose: Double, list: List<HisData>, infoView: KLineMarkView, vararg otherChart: Chart<*>) {
        mWidth = DisplayUtils.getWidthHeight(context)[0]
        mLastClose = lastClose
        mList = list
        mMarkView = infoView
        mOtherChart = otherChart
        mKlineView = klineView
    }

    override fun onValueSelected(e: Entry, h: Highlight) {
        val x = e.x.toInt()
        if (x < mList!!.size) {
            mInfoView?.visibility = View.VISIBLE
            mInfoView?.setData(mLastClose, mList!![x])
            mMarkView?.visibility = View.VISIBLE

            mMarkView?.update(mList!![x], h, x)
            mKlineView?.updateValueSelected(mList!![x])
        }
        val lp = mInfoView?.layoutParams as? FrameLayout.LayoutParams
        if (h.xPx < mWidth / 2) {
            lp?.gravity = Gravity.END
        } else {
            lp?.gravity = Gravity.START
        }
        mInfoView?.layoutParams = lp
        if (mOtherChart != null) {
            for (aMOtherChart in mOtherChart!!) {
                aMOtherChart.highlightValues(arrayOf(Highlight(h.x, java.lang.Float.NaN, h.dataSetIndex)))
            }
        }
    }

    override fun onNothingSelected() {
        mInfoView?.visibility = View.GONE
        mMarkView?.visibility = View.GONE
        if (mOtherChart != null) {
            for (i in mOtherChart?.indices!!) {
                mOtherChart!![i].highlightValues(null)
            }
        }
    }
}

