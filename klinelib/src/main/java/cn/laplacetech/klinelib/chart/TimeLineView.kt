package cn.laplacetech.klinelib.chart

import android.content.Context
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater

import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DataUtils
import cn.laplacetech.klinelib.util.DateUtils
import cn.laplacetech.klinelib.util.DisplayUtils
import cn.laplacetech.klinelib.util.DoubleUtil

import java.util.ArrayList
import java.util.Locale

/**
 * kline
 * Created by lilifeng@laplacetech.cn on 2017/10/26.
 */
class TimeLineView @JvmOverloads constructor(protected var mContext: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : BaseView(mContext, attrs, defStyleAttr), CoupleChartGestureListener.OnAxisChangeListener {

    protected var mChartPrice: CustomCombinedChart
    protected var mChartVolume: CustomCombinedChart

    protected var mChartInfoView: ChartInfoView

    /**
     * last price
     */
    private var mLastPrice: Double = 0.toDouble()

    /**
     * yesterday close price
     */
    private var mLastClose: Double = 0.toDouble()

    /**
     * the digits of the symbol
     */
    private val mDigits = 2


    override val lastData: HisData?
        get() = if (mData != null && !mData!!.isEmpty()) {
            mData!![mData!!.size - 1]
        } else null

    init {
        LayoutInflater.from(mContext).inflate(R.layout.view_timeline, this)
        mChartPrice = findViewById(R.id.price_chart)
        mChartVolume = findViewById(R.id.vol_chart)
        mChartInfoView = findViewById(R.id.line_info)

        mChartInfoView.setChart(mChartPrice, mChartVolume)

        mChartPrice.setNoDataText(mContext.getString(R.string.loading))
        initChartPrice()
        initBottomChart(mChartVolume)
        setOffset()
        initChartListener()
    }


    protected fun initChartPrice() {
        mChartPrice.setScaleEnabled(true)
        mChartPrice.setDrawBorders(false)
        mChartPrice.setBorderWidth(1f)
        mChartPrice.isDragEnabled = true
        mChartPrice.isScaleYEnabled = false
        mChartPrice.description.isEnabled = false
        mChartPrice.isAutoScaleMinMaxEnabled = true
        mChartPrice.isDragDecelerationEnabled = false
        val mvx = LineChartXMarkerView(mContext, mData)
        mvx.chartView = mChartPrice
        mChartPrice.setXMarker(mvx)
        val lineChartLegend = mChartPrice.legend
        lineChartLegend.isEnabled = false

        val xAxisPrice = mChartPrice.xAxis
        xAxisPrice.setDrawLabels(false)
        xAxisPrice.setDrawAxisLine(false)
        xAxisPrice.setDrawGridLines(false)
        xAxisPrice.axisMinimum = -0.5f


        val axisLeftPrice = mChartPrice.axisLeft
        axisLeftPrice.setLabelCount(5, true)
        axisLeftPrice.setDrawLabels(true)
        axisLeftPrice.setDrawGridLines(false)

        axisLeftPrice.setDrawAxisLine(false)
        axisLeftPrice.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        axisLeftPrice.textColor = mAxisColor
        axisLeftPrice.valueFormatter = IAxisValueFormatter { value, axis -> DoubleUtil.getStringByDigits(value.toDouble(), mDigits) }

        val colorArray = intArrayOf(mDecreasingColor, mDecreasingColor, mAxisColor, mIncreasingColor, mIncreasingColor)
        val leftYTransformer = mChartPrice.rendererLeftYAxis.transformer
        val leftColorContentYAxisRenderer = ColorContentYAxisRenderer(mChartPrice.viewPortHandler, mChartPrice.axisLeft, leftYTransformer)
        leftColorContentYAxisRenderer.setLabelColor(colorArray)
        leftColorContentYAxisRenderer.setLabelInContent(true)
        leftColorContentYAxisRenderer.setUseDefaultLabelXOffset(false)
        mChartPrice.rendererLeftYAxis = leftColorContentYAxisRenderer


        val axisRightPrice = mChartPrice.axisRight
        axisRightPrice.setLabelCount(5, true)
        axisRightPrice.setDrawLabels(true)

        axisRightPrice.setDrawGridLines(false)
        axisRightPrice.setDrawAxisLine(false)
        axisRightPrice.textColor = mAxisColor
        axisRightPrice.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)

        axisRightPrice.valueFormatter = IAxisValueFormatter { value, axis ->
            val rate = (value - mLastClose) / mLastClose * 100
            if (java.lang.Double.isNaN(rate) || java.lang.Double.isInfinite(rate)) {
                return@IAxisValueFormatter ""
            }
            val s = String.format(Locale.getDefault(), "%.2f%%",
                    rate)
            if (TextUtils.equals("-0.00%", s)) {
                "0.00%"
            } else s
        }

        //        设置标签Y渲染器
        val rightYTransformer = mChartPrice.rendererRightYAxis.transformer
        val rightColorContentYAxisRenderer = ColorContentYAxisRenderer(mChartPrice.viewPortHandler, mChartPrice.axisRight, rightYTransformer)
        rightColorContentYAxisRenderer.setLabelInContent(true)
        rightColorContentYAxisRenderer.setUseDefaultLabelXOffset(false)
        rightColorContentYAxisRenderer.setLabelColor(colorArray)
        mChartPrice.rendererRightYAxis = rightColorContentYAxisRenderer

    }


    private fun initChartListener() {
        mChartPrice.onChartGestureListener = CoupleChartGestureListener(this, mChartPrice, mChartVolume)
        mChartVolume.onChartGestureListener = CoupleChartGestureListener(this, mChartVolume, mChartPrice)
        mChartPrice.setOnChartValueSelectedListener(InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartVolume))
        mChartVolume.setOnChartValueSelectedListener(InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice))

        mChartPrice.setOnTouchListener(ChartInfoViewHandler(mChartPrice))
        mChartVolume.setOnTouchListener(ChartInfoViewHandler(mChartVolume))
    }


    fun initData(hisDatas: List<HisData>) {

        mData!!.clear()
        mData!!.addAll(DataUtils.calculateHisData(hisDatas))
        mChartPrice.realCount = mData!!.size

        val priceEntries = ArrayList<Entry>(INIT_COUNT)
        val aveEntries = ArrayList<Entry>(INIT_COUNT)
        val paddingEntries = ArrayList<Entry>(INIT_COUNT)

        for (i in mData!!.indices) {
            priceEntries.add(Entry(i.toFloat(), mData!![i].close!!.toFloat()))
            aveEntries.add(Entry(i.toFloat(), mData!![i].avePrice!!.toFloat()))
        }
        if (!mData!!.isEmpty() && mData!!.size < MAX_COUNT) {
            for (i in mData!!.size until MAX_COUNT) {
                paddingEntries.add(Entry(i.toFloat(), mData!![mData!!.size - 1].close!!.toFloat()))
            }
        }
        val sets = ArrayList<ILineDataSet>()
        sets.add(setLine(NORMAL_LINE, priceEntries))
        sets.add(setLine(AVE_LINE, aveEntries))
        sets.add(setLine(INVISIABLE_LINE, paddingEntries))
        val lineData = LineData(sets)

        val combinedData = CombinedData()
        combinedData.setData(lineData)
        mChartPrice.data = combinedData

        mChartPrice.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        mChartPrice.notifyDataSetChanged()
        //        mChartPrice.moveViewToX(combinedData.getEntryCount());
        moveToLast(mChartPrice)
        initChartVolumeData()

        mChartPrice.xAxis.axisMaximum = combinedData.xMax + 0.5f
        mChartVolume.xAxis.axisMaximum = mChartVolume.data.xMax + 0.5f

        mChartPrice.zoom(MAX_COUNT * 1f / INIT_COUNT, 0f, 0f, 0f)
        mChartVolume.zoom(MAX_COUNT * 1f / INIT_COUNT, 0f, 0f, 0f)

        setDescription(mChartVolume, "成交量 " + lastData!!.vol!!)
    }

    fun initDatas(vararg hisDatas: List<HisData>) {
        // 设置标签数量，并让标签居中显示
        val xAxis = mChartVolume.xAxis
        xAxis.setLabelCount(hisDatas.size, false)
        xAxis.setAvoidFirstLastClipping(false)
        xAxis.setCenterAxisLabels(true)
        xAxis.granularity = hisDatas[0].size.toFloat()
        xAxis.valueFormatter = IAxisValueFormatter { value, axis ->
            var value = value
            if (mData!!.isEmpty()) {
                return@IAxisValueFormatter ""
            }
            if (value < 0) {
                value = 0f
            }
            if (value < mData.size) {
                DateUtils.formatDate(mData[value.toInt()].date, mDateFormat)
            } else ""
        }
        mData.clear()
        val sets = ArrayList<ILineDataSet>()
        val barSets = ArrayList<IBarDataSet>()

        for (hisData in hisDatas) {
            val hisData = DataUtils.calculateHisData(hisData)
            val priceEntries = ArrayList<Entry>(INIT_COUNT)
            val aveEntries = ArrayList<Entry>(INIT_COUNT)
            val paddingEntries = ArrayList<Entry>(INIT_COUNT)
            val barPaddingEntries = ArrayList<BarEntry>(INIT_COUNT)
            val barEntries = ArrayList<BarEntry>(INIT_COUNT)

            for (i in hisData.indices) {
                val t = hisData[i]
                priceEntries.add(Entry((i + mData.size).toFloat(), t.close!!.toFloat()))
                aveEntries.add(Entry((i + mData.size).toFloat(), t.avePrice!!.toFloat()))
                barEntries.add(BarEntry((i + mData.size).toFloat(), t.vol!!.toFloat(), t))
            }
            if (!hisData.isEmpty() && hisData.size < INIT_COUNT / hisDatas.size) {
                for (i in hisData.size until INIT_COUNT / hisDatas.size) {
                    paddingEntries.add(Entry(i.toFloat(), hisData[hisData.size - 1].close!!.toFloat()))
                    barPaddingEntries.add(BarEntry(i.toFloat(), hisData[hisData.size - 1].close!!.toFloat()))
                }
            }
            sets.add(setLine(NORMAL_LINE_5DAY, priceEntries))
            sets.add(setLine(AVE_LINE, aveEntries))
            sets.add(setLine(INVISIABLE_LINE, paddingEntries))
            barSets.add(setBar(barEntries, NORMAL_LINE))
            barSets.add(setBar(barPaddingEntries, INVISIABLE_LINE))
            barSets.add(setBar(barPaddingEntries, INVISIABLE_LINE))
            mData!!.addAll(hisData)
            mChartPrice.realCount = mData!!.size
        }

        val lineData = LineData(sets)

        val combinedData = CombinedData()
        combinedData.setData(lineData)
        mChartPrice.data = combinedData
        mChartPrice.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        mChartPrice.notifyDataSetChanged()
        //        mChartPrice.moveViewToX(combinedData.getEntryCount());
        moveToLast(mChartVolume)


        val barData = BarData(barSets)
        barData.barWidth = 0.75f
        val combinedData2 = CombinedData()
        combinedData2.setData(barData)
        mChartVolume.data = combinedData2
        mChartVolume.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        mChartVolume.notifyDataSetChanged()
        mChartVolume.moveViewToX(combinedData2.entryCount.toFloat())

        mChartPrice.xAxis.axisMaximum = combinedData.xMax + 0.5f
        mChartVolume.xAxis.axisMaximum = mChartVolume.data.xMax + 0.5f

        mChartPrice.zoom(MAX_COUNT * 1f / INIT_COUNT, 0f, 0f, 0f)
        mChartVolume.zoom(MAX_COUNT * 1f / INIT_COUNT, 0f, 0f, 0f)

        setDescription(mChartVolume, "成交量 " + lastData!!.vol!!)
    }


    private fun setBar(barEntries: ArrayList<BarEntry>, type: Int): BarDataSet {
        val barDataSet = BarDataSet(barEntries, "vol")
        barDataSet.highLightAlpha = 255
        barDataSet.highLightColor = resources.getColor(R.color.highlight_color)
        barDataSet.setDrawValues(false)
        barDataSet.isVisible = type != INVISIABLE_LINE
        barDataSet.isHighlightEnabled = type != INVISIABLE_LINE
        barDataSet.setColors(resources.getColor(R.color.increasing_color), resources.getColor(R.color.decreasing_color))
        return barDataSet
    }


    private fun setLine(type: Int, lineEntries: ArrayList<Entry>): LineDataSet {
        val lineDataSetMa = LineDataSet(lineEntries, "ma$type")
        lineDataSetMa.setDrawValues(false)
        if (type == NORMAL_LINE) {
            lineDataSetMa.color = resources.getColor(R.color.normal_line_color)
            lineDataSetMa.setCircleColor(ContextCompat.getColor(mContext, R.color.normal_line_color))
        } else if (type == NORMAL_LINE_5DAY) {
            lineDataSetMa.color = resources.getColor(R.color.normal_line_color)
            lineDataSetMa.setCircleColor(mTransparentColor)
        } else if (type == AVE_LINE) {
            lineDataSetMa.color = resources.getColor(R.color.ave_color)
            lineDataSetMa.setCircleColor(mTransparentColor)
            lineDataSetMa.isHighlightEnabled = false
        } else {
            lineDataSetMa.isVisible = false
            lineDataSetMa.isHighlightEnabled = false
        }
        lineDataSetMa.axisDependency = YAxis.AxisDependency.LEFT
        lineDataSetMa.lineWidth = 1f
        lineDataSetMa.circleRadius = 1f

        lineDataSetMa.setDrawCircles(false)
        lineDataSetMa.setDrawCircleHole(false)

        return lineDataSetMa
    }


    private fun initChartVolumeData() {
        val barEntries = ArrayList<BarEntry>()
        val paddingEntries = ArrayList<BarEntry>()
        for (i in mData.indices) {
            val t = mData[i]
            barEntries.add(BarEntry(i.toFloat(), t.vol!!.toFloat(), t))
        }
        val maxCount = MAX_COUNT
        if (!mData.isEmpty() && mData.size < maxCount) {
            for (i in mData.size until maxCount) {
                paddingEntries.add(BarEntry(i.toFloat(), 0f))
            }
        }

        val barData = BarData(setBar(barEntries, NORMAL_LINE), setBar(paddingEntries, INVISIABLE_LINE))
        barData.barWidth = 0.75f
        val combinedData = CombinedData()
        combinedData.setData(barData)
        mChartVolume.data = combinedData

        mChartVolume.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        mChartVolume.notifyDataSetChanged()
        mChartVolume.moveViewToX(combinedData.entryCount.toFloat())

    }


    /**
     * according to the price to refresh the last data of the chart
     */
    fun refreshData(price: Float) {
        if (price <= 0 || price.toDouble() == mLastPrice) {
            return
        }
        mLastPrice = price.toDouble()
        val data = mChartPrice.data ?: return
        val lineData = data.lineData
        if (lineData != null) {
            val set = lineData.getDataSetByIndex(0)
            if (set!!.removeLast()) {
                set.addEntry(Entry(set.entryCount.toFloat(), price))
            }
        }

        mChartPrice.notifyDataSetChanged()
        mChartPrice.invalidate()
    }


    fun addData(hisData: HisData) {
        var hisData = hisData
        hisData = DataUtils.calculateHisData(hisData, mData)
        val combinedData = mChartPrice.data
        val priceData = combinedData.lineData
        val priceSet = priceData.getDataSetByIndex(0)
        val aveSet = priceData.getDataSetByIndex(1)
        val volSet = mChartVolume.data.barData.getDataSetByIndex(0)
        if (mData!!.contains(hisData)) {
            val index = mData!!.indexOf(hisData)
            priceSet!!.removeEntry(index)
            aveSet!!.removeEntry(index)
            volSet!!.removeEntry(index)
            mData!!.removeAt(index)
        }

        mData!!.add(hisData)
        mChartPrice.realCount = mData!!.size

        priceSet!!.addEntry(Entry(priceSet.entryCount.toFloat(), hisData.close!!.toFloat()))
        aveSet!!.addEntry(Entry(aveSet.entryCount.toFloat(), hisData.avePrice!!.toFloat()))
        volSet!!.addEntry(BarEntry(volSet.entryCount.toFloat(), hisData.vol!!.toFloat(), hisData))

        mChartPrice.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        mChartVolume.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        mChartPrice.xAxis.axisMaximum = combinedData.xMax + 1.5f
        mChartVolume.xAxis.axisMaximum = mChartVolume.data.xMax + 1.5f

        mChartPrice.notifyDataSetChanged()
        mChartPrice.invalidate()
        mChartVolume.notifyDataSetChanged()
        mChartVolume.invalidate()

        setDescription(mChartVolume, "成交量 " + hisData.vol!!)
    }


    /**
     * align two chart
     */
    private fun setOffset() {
        val chartHeight = resources.getDimensionPixelSize(R.dimen.bottom_chart_height)
        mChartPrice.setViewPortOffsets(0f, 0f, 0f, chartHeight.toFloat())
        mChartVolume.setViewPortOffsets(0f, 0f, 0f, DisplayUtils.dip2px(mContext, 14f).toFloat())
    }


    /**
     * add limit line to chart
     */
    @JvmOverloads
    fun setLimitLine(lastClose: Double = mLastClose) {
        val limitLine = LimitLine(lastClose.toFloat())
        limitLine.enableDashedLine(5f, 10f, 0f)
        limitLine.lineColor = resources.getColor(R.color.limit_color)
        mChartPrice.axisLeft.addLimitLine(limitLine)
    }

    fun setLastClose(lastClose: Double) {
        mLastClose = lastClose
        mChartPrice.setYCenter(lastClose.toFloat())
        mChartPrice.setOnChartValueSelectedListener(InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartVolume))
        mChartVolume.setOnChartValueSelectedListener(InfoViewListener(mContext, mLastClose, mData, mChartInfoView, mChartPrice))

    }


    override fun onAxisChange(chart: BarLineChartBase<*>) {
        val lowestVisibleX = chart.lowestVisibleX
        if (lowestVisibleX <= chart.xAxis.axisMinimum) return
        val maxX = chart.highestVisibleX.toInt()
        val x = Math.min(maxX, mData!!.size - 1)
        val hisData = mData!![if (x < 0) 0 else x]
        setDescription(mChartVolume, "成交量 " + hisData.vol!!)
    }

    companion object {


        val NORMAL_LINE = 0

        val NORMAL_LINE_5DAY = 5
        /**
         * average line
         */
        val AVE_LINE = 1
        /**
         * hide line
         */
        val INVISIABLE_LINE = 6
    }
}
