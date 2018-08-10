package cn.laplacetech.klinelib.chart

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.RelativeLayout

import com.github.mikephil.charting.BuildConfig
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DataUtils
import cn.laplacetech.klinelib.util.DateUtils
import kotlinx.android.synthetic.main.view_mp_line_chart.view.*

import java.util.ArrayList

/**
* Created by lilifeng@laplacetech.cn on 2018/8/6.
*
*/
@Deprecated("")
class TickChart : RelativeLayout {
    private val mList = ArrayList<HisData>()
//    private lateinit var mContext: Context
    private val mLineColor = resources.getColor(R.color.normal_line_color)
    private val mHighlightColor = resources.getColor(R.color.highlight_color)
    private val transparentColor = resources.getColor(android.R.color.transparent)
    private val candleGridColor = resources.getColor(R.color.chart_grid_color)
    private val mTextColor = resources.getColor(R.color.axis_color)

    private var mLastPrice: Float = 0f


    private val xValueFormatter = IAxisValueFormatter { value, axis ->
        if (value < mList.size) {
            DateUtils.formatTime(mList[value.toInt()].date)
        } else ""
    }

//    private var mInfoView: LineChartInfoView? = null

    val chart: LineChart?
        get() = line_chart


    val lastData: HisData?
        get() {
            try {
                return mList[mList.size - 1]
            } catch (e: Exception) {
                return null
            }

        }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
//        mContext = context
        LayoutInflater.from(context).inflate(R.layout.view_mp_line_chart, this)
        setupSettingParameter()
    }

    fun addEntries(list: List<HisData>) {

        mList.clear()
        mList.addAll(list)
        val data = LineData()
        var setSell = data.getDataSetByIndex(DATA_SET_PRICE)
        if (setSell == null) {
            setSell = createSet(TYPE_FULL)
            data.addDataSet(setSell)
        }

        var paddingSet = data.getDataSetByIndex(DATA_SET_PADDING)
        if (paddingSet == null) {
            paddingSet = createSet(DATA_SET_PADDING)
            data.addDataSet(paddingSet)
        }

        var aveSet = data.getDataSetByIndex(DATA_SET_AVE)
        if (aveSet == null) {
            aveSet = createSet(DATA_SET_AVE)
            data.addDataSet(aveSet)
        }

        for (i in mList.indices) {
            val hisData = mList[i]
            data.addEntry(Entry(i.toFloat(), hisData.avePrice!!.toFloat()), DATA_SET_AVE)
            data.addEntry(Entry(setSell.entryCount.toFloat(), hisData.close!!.toFloat()), DATA_SET_PRICE)
        }

        val size: Int
        if (mList.size < FULL_SCREEN_SHOW_COUNT - PADDING_COUNT) {
            size = FULL_SCREEN_SHOW_COUNT
        } else {
            size = mList.size + PADDING_COUNT
        }

        for (i in mList.size until size) {
            data.addEntry(Entry(i.toFloat(), mList[mList.size - 1].close!!.toFloat()), DATA_SET_PADDING)
        }

        line_chart.data = data

        val chartHighlighter = Highlight((setSell.entryCount + paddingSet.entryCount).toFloat(), mList[mList.size - 1].close!!.toFloat(), DATA_SET_PADDING)
        line_chart.highlightValue(chartHighlighter)

        line_chart.notifyDataSetChanged()
        line_chart.invalidate()

        val port = line_chart.viewPortHandler
        line_chart.setViewPortOffsets(0f, port.offsetTop(), port.offsetRight(), port.offsetBottom())

        line_chart.moveViewToX(data.entryCount.toFloat())
        line_chart.setVisibleXRange(FULL_SCREEN_SHOW_COUNT.toFloat(), 50f)
    }


    fun refreshData(price: Float) {
        if (price <= 0 || price == mLastPrice) {
            return
        }
        mLastPrice = price
        val data = line_chart.data

        if (data != null) {
            var setSell = data.getDataSetByIndex(DATA_SET_PRICE)
            if (setSell == null) {
                setSell = createSet(TYPE_FULL)
                data.addDataSet(setSell)
            }

            data.removeEntry(setSell.entryCount.toFloat(), DATA_SET_PRICE)
            val entry = Entry(setSell.entryCount.toFloat(), price)
            data.addEntry(entry, DATA_SET_PRICE)

            var paddingSet = data.getDataSetByIndex(DATA_SET_PADDING)
            if (paddingSet == null) {
                paddingSet = createSet(TYPE_DASHED)
                data.addDataSet(paddingSet)
            }

            val count = paddingSet.entryCount
            paddingSet.clear()
            for (i in 0 until count) {
                paddingSet.addEntry(Entry((setSell.entryCount + i).toFloat(), price))
            }

            val chartHighlighter = Highlight((setSell.entryCount + paddingSet.entryCount).toFloat(), price, DATA_SET_PADDING)
            line_chart.highlightValue(chartHighlighter)

            data.notifyDataChanged()
            line_chart.notifyDataSetChanged()
            line_chart.invalidate()


        }
    }


    fun addEntry(hisData: HisData) {
        var hisData = hisData
        hisData = DataUtils.calculateHisData(hisData, mList)
        val data = line_chart.data

        if (data != null) {
            var setSell = data.getDataSetByIndex(DATA_SET_PRICE)
            if (setSell == null) {
                setSell = createSet(TYPE_FULL)
                data.addDataSet(setSell)
            }
            var aveSet = data.getDataSetByIndex(DATA_SET_AVE)
            if (aveSet == null) {
                aveSet = createSet(DATA_SET_AVE)
                data.addDataSet(aveSet)
            }

            val index = mList.indexOf(hisData)
            if (index >= 0) {
                mList.remove(hisData)
                data.removeEntry(index.toFloat(), DATA_SET_PRICE)
                data.removeEntry(index.toFloat(), DATA_SET_AVE)
            }
            mList.add(hisData)
            val price = hisData.close!!.toFloat()
            data.addEntry(Entry(setSell.entryCount.toFloat(), price), DATA_SET_PRICE)
            data.addEntry(Entry(setSell.entryCount.toFloat(), hisData.avePrice!!.toFloat()), DATA_SET_AVE)

            var paddingSet = data.getDataSetByIndex(DATA_SET_PADDING)
            if (paddingSet == null) {
                paddingSet = createSet(TYPE_DASHED)
                data.addDataSet(paddingSet)
            }

            var count = paddingSet.entryCount

            if (count > PADDING_COUNT && index < 0) {
                count--
            }
            paddingSet.clear()
            for (i in 0 until count) {
                paddingSet.addEntry(Entry((setSell.entryCount + i).toFloat(), price))
            }

            val chartHighlighter = Highlight((setSell.entryCount + paddingSet.entryCount).toFloat(), price, DATA_SET_PADDING)
            line_chart.highlightValue(chartHighlighter)

            data.notifyDataChanged()
            line_chart.notifyDataSetChanged()
            line_chart.invalidate()
        }
    }

    private fun createSet(type: Int): ILineDataSet {
        val set = LineDataSet(null, type.toString())
        //        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        when (type) {
            TYPE_FULL -> {
                set.highLightColor = mHighlightColor
                set.setDrawHighlightIndicators(true)
                //            set.setDrawVerticalHighlightIndicator(false);
                set.highlightLineWidth = 0.5f
                set.setCircleColor(mLineColor)
                set.circleRadius = 1.5f
                set.setDrawCircleHole(false)
                set.setDrawFilled(true)
                set.color = mLineColor
                set.lineWidth = 1f
                set.fillDrawable = ColorDrawable(transparentColor)
            }
            TYPE_AVE -> {
                set.isHighlightEnabled = true
                set.color = ContextCompat.getColor(context, R.color.ave_color)
                set.lineWidth = 1f
                set.circleRadius = 1.5f
                set.setDrawCircleHole(false)
                set.setCircleColor(transparentColor)
                set.lineWidth = 0.5f
            }
            else -> {
                set.isHighlightEnabled = true
                set.setDrawVerticalHighlightIndicator(false)
                set.highLightColor = transparentColor
                set.color = mLineColor
                set.enableDashedLine(3f, 40f, 0f)
                set.setDrawCircleHole(false)
                set.setCircleColor(transparentColor)
                set.lineWidth = 1f
                set.isVisible = true
            }
        }
        set.setDrawCircles(false)
        set.setDrawValues(false)
        return set
    }

    private fun setupSettingParameter() {
        line_chart.setDrawGridBackground(false)
        val mvx = LineChartXMarkerView(context, mList)
        mvx.chartView = line_chart
        line_chart.setXMarker(mvx)
        line_chart.setNoDataText(context.getString(R.string.loading))
        line_chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.chart_no_data_color))
        line_chart.description.isEnabled = false
        line_chart.setPinchZoom(false)
        line_chart.isScaleYEnabled = false
        line_chart.isAutoScaleMinMaxEnabled = true
        line_chart.isLogEnabled = BuildConfig.DEBUG

        val mv = LineChartYMarkerView(context)
        mv.chartView = line_chart
        line_chart.marker = mv
        line_chart.setOnChartValueSelectedListener(InfoViewListener(context, 56.86, mList, info))
        line_chart.setOnTouchListener(ChartInfoViewHandler(line_chart))
        line_chart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(event: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {}

            override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
                line_chart.isDragEnabled = true
            }

            override fun onChartLongPressed(me: MotionEvent) {
                //                mChart.setDragEnabled(false);
            }

            override fun onChartDoubleTapped(me: MotionEvent) {}

            override fun onChartSingleTapped(me: MotionEvent) {}

            override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {}

            override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {}

            override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {}
        }


        val rightAxis = line_chart.axisRight
        rightAxis.setDrawGridLines(true)
        rightAxis.gridColor = candleGridColor
        rightAxis.textColor = mTextColor
        rightAxis.gridLineWidth = 0.5f
        rightAxis.enableGridDashedLine(5f, 5f, 0f)
        rightAxis.setLabelCount(6, true)
        rightAxis.setDrawAxisLine(false)

        //        rightAxis.setValueFormatter(new YValueFormatter(2));
        val legend = line_chart.legend
        legend.isEnabled = false

        val leftAxis = line_chart.axisLeft
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        leftAxis.setDrawLabels(false)
        leftAxis.setDrawAxisLine(false)
        leftAxis.setDrawGridLines(false)

        val xAxis = line_chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawGridLines(false)
        xAxis.textColor = mTextColor
        xAxis.gridColor = candleGridColor
        xAxis.setLabelCount(5, true)
        xAxis.setAvoidFirstLastClipping(true)

        xAxis.valueFormatter = xValueFormatter

    }

    fun setNoDataText(text: String) {
        line_chart.setNoDataText(text)
    }

    companion object {

        val TYPE_FULL = 0

        val TYPE_DASHED = 1

        val TYPE_AVE = 2

        val FULL_SCREEN_SHOW_COUNT = 160
        val PADDING_COUNT = 30
        val DATA_SET_PRICE = 0
        val DATA_SET_PADDING = 1
        val DATA_SET_AVE = 2
    }
}
