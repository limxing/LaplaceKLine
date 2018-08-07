package cn.laplacetech.klinelib.chart

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

class CustomLineChart @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LineChart(context, attrs, defStyle) {

    private var mXMarker: IMarker? = null

    override fun init() {
        super.init()
        mRenderer = CustomLineChartRenderer(this, mAnimator, mViewPortHandler)
    }

    fun setXMarker(marker: IMarker) {
        mXMarker = marker
    }

    override fun drawMarkers(canvas: Canvas) {
        if (mMarker == null || !isDrawMarkersEnabled || !valuesToHighlight())
            return

        for (i in mIndicesToHighlight.indices) {

            val highlight = mIndicesToHighlight[i]

            val set = mData.getDataSetByIndex(highlight.dataSetIndex)

            val e = mData.getEntryForHighlight(mIndicesToHighlight[i])
            val entryIndex = set!!.getEntryIndex(e)

            // make sure entry not null
            if (e == null || entryIndex > set.entryCount * mAnimator.phaseX)
                continue

            val pos = getMarkerPosition(highlight)

            // check bounds
            if (!mViewPortHandler.isInBounds(pos[0], pos[1]))
                continue

            // callbacks to update the content
            mMarker.refreshContent(e, highlight)
            if (mXMarker != null && set.isVerticalHighlightIndicatorEnabled) {
                mXMarker!!.refreshContent(e, highlight)
            }

            // draw the marker
            //            if (mMarker instanceof LineChartYMarkerView) {
            val yMarker = mMarker as LineChartYMarkerView
            val xMarker = mXMarker as LineChartXMarkerView?
            val width = yMarker.measuredWidth
            mMarker.draw(canvas, measuredWidth - width * 1.05f, pos[1] - yMarker.measuredHeight / 2)

            if (mXMarker != null && set.isVerticalHighlightIndicatorEnabled) {
                mXMarker!!.draw(canvas, pos[0] - xMarker!!.measuredWidth / 2, measuredHeight.toFloat())
            }
            //            } else {
            //                mMarker.draw(canvas, pos[0], pos[1]);
            //            }
        }
    }

    override fun highlightValue(highlight: Highlight) {
        super.highlightValue(highlight)
    }

}
