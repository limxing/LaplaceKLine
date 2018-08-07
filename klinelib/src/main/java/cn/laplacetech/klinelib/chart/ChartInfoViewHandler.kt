package cn.laplacetech.klinelib.chart

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.highlight.Highlight

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

class ChartInfoViewHandler(private val mChart: BarLineChartBase<*>) : View.OnTouchListener {
    private val mDetector: GestureDetector

    private var mIsLongPress = false

    init {
        mDetector = GestureDetector(mChart.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                mIsLongPress = true
                val h = mChart.getHighlightByTouchPoint(e.x, e.y)
                if (h != null) {
                    mChart.highlightValue(h, true)
                    mChart.disableScroll()
                }
            }

        })
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            mIsLongPress = false
        }
        if (mIsLongPress && event.action == MotionEvent.ACTION_MOVE) {
            val h = mChart.getHighlightByTouchPoint(event.x, event.y)
            if (h != null) {
                mChart.highlightValue(h, true)
                mChart.disableScroll()
            }
            return true
        }
        return false
    }
}
