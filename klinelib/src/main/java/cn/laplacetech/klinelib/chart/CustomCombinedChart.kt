package cn.laplacetech.klinelib.chart

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log

import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.IMarker
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.DataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.utils.MPPointF

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

class CustomCombinedChart @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : CombinedChart(context, attrs, defStyle) {

    private var mXMarker: IMarker? = null
    private var mYMarker: IMarker? = null

    private var mYCenter: Float = 0.toFloat()

    override fun init() {
        super.init()
        mRenderer = CustomCombinedChartRenderer(this, mAnimator, mViewPortHandler)
        isLogEnabled = false
    }

    fun setYMarker(marker: IMarker) {
        mYMarker = marker
    }

    fun setXMarker(marker: IMarker) {
        mXMarker = marker
    }

    override fun setData(data: CombinedData) {
        try {
            super.setData(data)
        } catch (e: ClassCastException) {
            // ignore
        }

        (mRenderer as CustomCombinedChartRenderer).createRenderers()
        mRenderer.initBuffers()
    }

    override fun drawMarkers(canvas: Canvas) {
        super.drawMarkers(canvas)
        if (mXMarker == null || !isDrawMarkersEnabled || !valuesToHighlight())
            return

        for (i in mIndicesToHighlight.indices) {

            val highlight = mIndicesToHighlight[i]

            val set = mData.getDataSetByIndex(highlight.dataSetIndex)

            val e = mData.getEntryForHighlight(mIndicesToHighlight[i])
//            有疑问，修改前是：
//            val entryIndex = set.getEntryIndex(e)
//            val entryIndex = set.getEntryIndex(e.x, e.y, DataSet.Rounding.CLOSEST)

            // make sure entry not null
            if (e == null || set.getEntryIndex(e.x, e.y, DataSet.Rounding.CLOSEST) > set.entryCount * mAnimator.phaseX)
                continue

            val pos = getMarkerPosition(highlight)

            // check bounds
            if (!mViewPortHandler.isInBounds(pos[0], pos[1]))
                continue

            // callbacks to update the content
//            mMarker.refreshContent(e, highlight)

            mXMarker?.refreshContent(e, highlight)
            mYMarker?.refreshContent(e, highlight)

            // draw the marker
            //            if (mMarker instanceof LineChartYMarkerView) {
//            val yMarker = mMarker as LineChartYMarkerView
            val xMarker = mXMarker as LineChartXMarkerView?
            val yMarker = mYMarker as LineChartYMarkerView?
//            val width = yMarker.measuredWidth
//            mMarker.draw(canvas, measuredWidth - width * 1.05f, pos[1] - yMarker.measuredHeight / 2)

            mXMarker?.draw(canvas, pos[0] - xMarker!!.measuredWidth / 2, measuredHeight.toFloat() - xMarker.measuredHeight)
            yMarker?.draw(canvas, 0f, pos[1] - yMarker.measuredHeight / 2)
            //            } else {
            //                mMarker.draw(canvas, pos[0], pos[1]);
            //            }
        }
    }

    override fun drawDescription(c: Canvas) {

        // check if description should be drawn
        if (mDescription != null && mDescription.isEnabled) {

            val position = mDescription.position

            mDescPaint.typeface = mDescription.typeface
            mDescPaint.textSize = mDescription.textSize
            mDescPaint.color = mDescription.textColor
            mDescPaint.textAlign = mDescription.textAlign

            val x: Float
            val y: Float

            // if no position specified, draw on default position
            if (position == null) {
                x = width.toFloat() - mViewPortHandler.offsetRight() - mDescription.xOffset
                y = mDescription.textSize + mViewPortHandler.offsetTop() + mDescription.yOffset
            } else {
                x = position.x
                y = position.y
            }

            c.drawText(mDescription.text, x, y, mDescPaint)
        }
    }


    /**
     * 重写这两个方法，为了让开盘价和涨跌幅剧中显示
     * Performs auto scaling of the axis by recalculating the minimum and maximum y-values based on the entries currently in view.
     */
    override fun autoScale() {

        val fromX = lowestVisibleX
        val toX = highestVisibleX

        mData.calcMinMaxY(fromX, toX)

        mXAxis.calculate(mData.xMin, mData.xMax)

        // calculate axis range (min / max) according to provided data

        if (mAxisLeft.isEnabled) {
            if (mYCenter == 0f) {
                mAxisLeft.calculate(mData.getYMin(YAxis.AxisDependency.LEFT),
                        mData.getYMax(YAxis.AxisDependency.LEFT))
            } else {
                var yMin = mData.getYMin(YAxis.AxisDependency.LEFT)
                var yMax = mData.getYMax(YAxis.AxisDependency.LEFT)
                val interval = Math.max(Math.abs(mYCenter - yMax), Math.abs(mYCenter - yMin))
                yMax = Math.max(yMax, mYCenter + interval)
                yMin = Math.min(yMin, mYCenter - interval)
                mAxisLeft.calculate(yMin, yMax)
            }
        }

        if (mAxisRight.isEnabled) {
            if (mYCenter == 0f) {
                mAxisRight.calculate(mData.getYMin(YAxis.AxisDependency.RIGHT),
                        mData.getYMax(YAxis.AxisDependency.RIGHT))
            } else {
                var yMin = mData.getYMin(YAxis.AxisDependency.RIGHT)
                var yMax = mData.getYMax(YAxis.AxisDependency.RIGHT)
                val interval = Math.max(Math.abs(mYCenter - yMax), Math.abs(mYCenter - yMin))
                yMax = Math.max(yMax, mYCenter + interval)
                yMin = Math.min(yMin, mYCenter - interval)
                mAxisRight.calculate(yMin, yMax)
            }
        }

        calculateOffsets()
    }

    /**
     * 重写这两个方法，为了让开盘价和涨跌幅剧中显示
     */
    override fun calcMinMax() {

        mXAxis.calculate(mData.xMin, mData.xMax)

        if (mYCenter == 0f) {
            // calculate axis range (min / max) according to provided data
            mAxisLeft.calculate(mData.getYMin(YAxis.AxisDependency.LEFT), mData.getYMax(YAxis.AxisDependency.LEFT))
            mAxisRight.calculate(mData.getYMin(YAxis.AxisDependency.RIGHT), mData.getYMax(YAxis.AxisDependency
                    .RIGHT))
        } else {
            var yLMin = mData.getYMin(YAxis.AxisDependency.LEFT)
            var yLMax = mData.getYMax(YAxis.AxisDependency.LEFT)
            val interval = Math.max(Math.abs(mYCenter - yLMax), Math.abs(mYCenter - yLMin))
            yLMax = Math.max(yLMax, mYCenter + interval)
            yLMin = Math.min(yLMin, mYCenter - interval)
            mAxisLeft.calculate(yLMin, yLMax)

            var yRMin = mData.getYMin(YAxis.AxisDependency.RIGHT)
            var yRMax = mData.getYMax(YAxis.AxisDependency.RIGHT)
            val rinterval = Math.max(Math.abs(mYCenter - yRMax), Math.abs(mYCenter - yRMin))
            yRMax = Math.max(yRMax, mYCenter + rinterval)
            yRMin = Math.min(yRMin, mYCenter - rinterval)
            mAxisRight.calculate(yRMin, yRMax)
        }
    }

    /**
     * 设置图表中Y居中的值
     */
    fun setYCenter(YCenter: Float) {
        mYCenter = YCenter
    }

    fun getYMarkView(): IMarker? {
        return mYMarker
    }

    fun getXMarkView(): IMarker? {
        return mXMarker
    }
}
