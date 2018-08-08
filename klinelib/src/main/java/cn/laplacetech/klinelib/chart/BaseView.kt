package cn.laplacetech.klinelib.chart

import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.widget.LinearLayout

import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.utils.Transformer
import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DateUtils
import cn.laplacetech.klinelib.util.DoubleUtil
import cn.laplacetech.klinelib.util.getColor
import com.orhanobut.logger.Logger

import java.util.ArrayList

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

open class BaseView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    var mDateFormat = "yyyy-MM-dd"

    protected var mDecreasingColor: Int = 0
    protected var mIncreasingColor: Int = 0
    protected var mAxisColor: Int = 0
    protected var mTransparentColor: Int = 0

    val MAX_COUNT_FLAG = 150

    var MAX_COUNT = MAX_COUNT_FLAG

    var MIN_COUNT = 25
    var INIT_COUNT = 50

    var isRedDown = false
        set(value) {
            if (value){
                mDecreasingColor = ContextCompat.getColor(context, R.color.increasing_color)
                mIncreasingColor = ContextCompat.getColor(context, R.color.decreasing_color)
            }else{
                mDecreasingColor = ContextCompat.getColor(context, R.color.decreasing_color)
                mIncreasingColor = ContextCompat.getColor(context, R.color.increasing_color)
            }
        }

    protected var mData: ArrayList<HisData> = ArrayList<HisData>(300)

    open val lastData: HisData?
        get() = if (!mData.isEmpty()) {
            mData.last()
        } else null

    init {
        mAxisColor = ContextCompat.getColor(getContext(), R.color.axis_color)
        mTransparentColor = ContextCompat.getColor(getContext(), android.R.color.transparent)
        mDecreasingColor = ContextCompat.getColor(getContext(), R.color.decreasing_color)
        mIncreasingColor = ContextCompat.getColor(getContext(), R.color.increasing_color)
    }

    protected fun initBottomChart(chart: CustomCombinedChart) {
        chart.setNoDataText("")
        chart.setScaleEnabled(true)
        chart.setDrawBorders(true)
        chart.setBorderWidth(0.5f)
        chart.setBorderColor(getColor(R.color.chart_border))
        chart.isDragEnabled = true
        chart.isScaleYEnabled = false
        chart.isAutoScaleMinMaxEnabled = true
        chart.isDragDecelerationEnabled = false
        chart.isHighlightPerDragEnabled = false
        val lineChartLegend = chart.legend
        lineChartLegend.isEnabled = false


        val xAxisVolume = chart.xAxis
        xAxisVolume.setDrawLabels(false)
        xAxisVolume.setDrawAxisLine(false)
        xAxisVolume.setDrawGridLines(false)
        xAxisVolume.textColor = mAxisColor
        xAxisVolume.position = XAxis.XAxisPosition.BOTTOM
        xAxisVolume.setLabelCount(3, true)
        xAxisVolume.setAvoidFirstLastClipping(true)
        xAxisVolume.axisMinimum = -0.5f

        xAxisVolume.valueFormatter = IAxisValueFormatter { valueR, _ ->
            var value = valueR
            if (mData.isEmpty()) {
                return@IAxisValueFormatter ""
            }
            if (value < 0) {
                value = 0f
            }
            if (value < mData.size) {
                DateUtils.formatDate(mData[value.toInt()].date, mDateFormat)
            } else ""
        }

        /**
         * 左侧Y轴的数字格式化
         */
        val axisLeftVolume = chart.axisLeft
        axisLeftVolume.setDrawLabels(true)
        axisLeftVolume.setDrawGridLines(false)
        axisLeftVolume.setLabelCount(3, true)
        axisLeftVolume.setDrawAxisLine(false)
        axisLeftVolume.textColor = mAxisColor
        axisLeftVolume.spaceTop = 10f
        axisLeftVolume.spaceBottom = 0f
        axisLeftVolume.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        axisLeftVolume.setValueFormatter { value, _ ->
            DoubleUtil.amountConversion(value.toDouble())
        }

        val leftYTransformer = chart.rendererLeftYAxis.transformer
        val leftColorContentYAxisRenderer = ColorContentYAxisRenderer(chart.viewPortHandler, chart.axisLeft, leftYTransformer)
        leftColorContentYAxisRenderer.setLabelInContent(true)
        leftColorContentYAxisRenderer.setUseDefaultLabelXOffset(false)
        chart.rendererLeftYAxis = leftColorContentYAxisRenderer

        //右边y
        val axisRightVolume = chart.axisRight
        axisRightVolume.setDrawLabels(false)
        axisRightVolume.setDrawGridLines(false)
        axisRightVolume.setDrawAxisLine(false)

    }


    private val TAG: String = "BaseKLineView"

    protected fun moveToLast(chart: CustomCombinedChart) {
        val maxX = chart.highestVisibleX.toInt()

        if (mData.size > INIT_COUNT) {
            chart.moveViewToX((mData.size - INIT_COUNT).toFloat())
//            chart.moveViewToAnimated((mData.size - INIT_COUNT).toFloat(),0f, YAxis.AxisDependency.RIGHT,300)
        } else {
            chart.moveViewToX(0f)
        }
    }

    /**
     * set the count of k chart
     */
    fun setCount(init: Int, max: Int, min: Int) {
        INIT_COUNT = init
        MAX_COUNT = max
        MIN_COUNT = min
    }

    protected fun setDescription(chart: Chart<*>, text: String) {
        val description = chart.description
        description.text = text
    }

    fun setDateFormat(mDateFormat: String) {
        this.mDateFormat = mDateFormat
    }


}
