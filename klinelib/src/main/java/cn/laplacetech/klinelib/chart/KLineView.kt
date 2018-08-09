package cn.laplacetech.klinelib.chart

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View

import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DataUtils
import cn.laplacetech.klinelib.util.DateUtils
import cn.laplacetech.klinelib.util.DisplayUtils
import cn.laplacetech.klinelib.util.DoubleUtil
import com.orhanobut.logger.Logger
import kotlinx.android.synthetic.main.view_kline.view.*

import java.util.ArrayList
import java.util.Locale

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class KLineView @JvmOverloads constructor(protected var mContext: Context, attrs: AttributeSet? = null,
                                          defStyleAttr: Int = 0) : BaseView(mContext, attrs, defStyleAttr),
        CoupleChartGestureListener.OnAxisChangeListener {


    private var kLineViewListener: KLineViewListener? = null

    /**
     * last price
     */
    private var mLastPrice: Double = 0.0

    /**
     * yesterday close price
     */
    private var mLastClose: Double = 0.0

    /**
     * the digits of the symbol
     */
    private val mDigits = 2
    private var mCoupleChartGestureListener: CoupleChartGestureListener? = null

    init {
        LayoutInflater.from(mContext).inflate(R.layout.view_kline, this)
        k_info_mark.setChart(price_chart, vol_chart, macd_chart, kdj_chart)

        price_chart.setNoDataText(mContext.getString(R.string.loading))
        initChartPrice()
        initBottomChart(vol_chart)
        initBottomChart(macd_chart)
        initBottomChart(kdj_chart)
        setOffset()
        initChartListener()
        showVolume()
//        Logger.addLogAdapter(AndroidLogAdapter())
        price_chart.isLogEnabled = false
        vol_chart.isLogEnabled = false
        if (mData.size == 0)
            postDelayed({
                val time = System.currentTimeMillis()
                val list = ArrayList<HisData>()
                val day = 60 * 60 * 24 * 1000
                for (i in 0..MAX_COUNT_FLAG * 2) {
                    val hisData = HisData()
                    hisData.date = time - day * i
                    list.add(hisData)
                }
                list.reverse()
                initData(list)

            }, 100)
    }

    fun showKdj() {
        vol_chart.visibility = View.GONE
        macd_chart.visibility = View.GONE
        kdj_chart.visibility = View.VISIBLE
    }

    fun showMacd() {
        vol_chart.visibility = View.GONE
        macd_chart.visibility = View.VISIBLE
        kdj_chart.visibility = View.GONE
    }

    fun showVolume() {
        macd_chart.visibility = View.GONE
        kdj_chart.visibility = View.GONE
        vol_chart.visibility = View.VISIBLE
    }

    /**
     * 初始化K线图
     */
    protected fun initChartPrice() {
        price_chart.setScaleEnabled(true)
        price_chart.setDrawBorders(true)//边框
        price_chart.setBorderWidth(0.5f)
        price_chart.setBorderColor(resources.getColor(R.color.chart_border))
        price_chart.isDragEnabled = true
        price_chart.isScaleYEnabled = false
        price_chart.isAutoScaleMinMaxEnabled = true//没用到啊
        price_chart.isDragDecelerationEnabled = false//是否滑动
        price_chart.isHighlightPerDragEnabled = false
        price_chart.description.isEnabled = false
        val mvx = LineChartXMarkerView(mContext, mData)
        mvx.chartView = price_chart
        price_chart.setXMarker(mvx)
        val lineChartLegend = price_chart.legend
        lineChartLegend.isEnabled = false


        //顶部X轴
        val xAxisPrice = price_chart.xAxis
        xAxisPrice.setDrawLabels(true)//
        xAxisPrice.setDrawAxisLine(false)//x轴 轴线
        xAxisPrice.setDrawGridLines(false)
        xAxisPrice.gridColor = Color.RED
        xAxisPrice.axisMinimum = -0.5f
        xAxisPrice.setAvoidFirstLastClipping(true)
        xAxisPrice.setLabelCount(4, true)
        xAxisPrice.textColor = mAxisColor
        xAxisPrice.position = XAxis.XAxisPosition.BOTTOM
        xAxisPrice.valueFormatter = IAxisValueFormatter { value, axis ->
            var value = value
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

        //横向网格，Y轴数据，左边的Y轴
        val axisLeftPrice = price_chart.axisLeft
        axisLeftPrice.setLabelCount(5, true)
        axisLeftPrice.setDrawLabels(true)
        axisLeftPrice.setDrawGridLines(true)//横向的网格 Y值
        axisLeftPrice.gridColor = resources.getColor(R.color.chart_border)//网格颜色
        axisLeftPrice.setDrawAxisLine(false)//Y 轴轴线
        axisLeftPrice.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)
        axisLeftPrice.textColor = mAxisColor
        axisLeftPrice.valueFormatter = IAxisValueFormatter { value, axis ->
            DoubleUtil.amountConversion(value.toDouble(), true)
        }
        axisLeftPrice.yOffset = -5f// Y轴的标签是否偏移，默认在线上

        val colorArray = intArrayOf(mDecreasingColor, mDecreasingColor, mAxisColor, mIncreasingColor, mIncreasingColor)

        val leftYTransformer = price_chart.rendererLeftYAxis.transformer
        val leftColorContentYAxisRenderer = ColorContentYAxisRenderer(price_chart.viewPortHandler, price_chart.axisLeft, leftYTransformer)
        leftColorContentYAxisRenderer.setLabelColor(colorArray)
        leftColorContentYAxisRenderer.setLabelInContent(true)
        leftColorContentYAxisRenderer.setUseDefaultLabelXOffset(false)
        //        price_chart.setRendererLeftYAxis(leftColorContentYAxisRenderer);//设置为Y轴标签是否是不同的颜色

        //右边的Y轴
        val axisRightPrice = price_chart.axisRight
        axisRightPrice.setLabelCount(5, true)
        axisRightPrice.setDrawLabels(false)

        axisRightPrice.setDrawGridLines(false)
        axisRightPrice.setDrawAxisLine(false)

        axisRightPrice.textColor = mAxisColor
        axisRightPrice.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART)

        axisRightPrice.valueFormatter = IAxisValueFormatter { value, axis ->
            val rate = (value - mLastClose) / mLastClose * 100
            if (java.lang.Double.isNaN(rate) || java.lang.Double.isInfinite(rate)) {
                return@IAxisValueFormatter "0.00%"
            }
            val s = String.format(Locale.getDefault(), "%.2f%%",
                    rate)
            if (TextUtils.equals("-0.00%", s)) {
                "0.00%"
            } else s
        }

        //        设置标签Y渲染器
        val rightYTransformer = price_chart.rendererRightYAxis.transformer
        val rightColorContentYAxisRenderer = ColorContentYAxisRenderer(price_chart.viewPortHandler, price_chart.axisRight, rightYTransformer)
        rightColorContentYAxisRenderer.setLabelInContent(true)
        rightColorContentYAxisRenderer.setUseDefaultLabelXOffset(false)
        rightColorContentYAxisRenderer.setLabelColor(colorArray)
        price_chart.rendererRightYAxis = rightColorContentYAxisRenderer

    }


    private fun initChartListener() {
        mCoupleChartGestureListener = CoupleChartGestureListener(this, price_chart, vol_chart, macd_chart, kdj_chart)
        price_chart.onChartGestureListener = mCoupleChartGestureListener
        price_chart.setOnChartValueSelectedListener(InfoViewListener(this, context, mLastClose, mData, k_info_mark, vol_chart, macd_chart, kdj_chart))
        price_chart.setOnTouchListener(ChartInfoViewHandler(price_chart))
    }


    private var lastDataCount: Int = 0

    /**
     * 初始化方法
     */
    fun initData(hisDatas: List<HisData>) {
        mData.clear()
        mData.addAll(DataUtils.calculateHisData(hisDatas))
//        if (mData.size< MAX_COUNT_FLAG){
//            setCount(INIT_COUNT, MAX_COUNT_FLAG, MIN_COUNT)//最大就是集合的大小，初始化当前
//        }else {
        setCount(INIT_COUNT, mData.size, MIN_COUNT)//最大就是集合的大小，初始化当前
//        }
        price_chart.realCount = mData.size

        Logger.i("数组的大小：${mData.size}")


        initChartPriceData()
        initChartVolumeData()
//        initChartMacdData()
//        initChartKdjData()


//        macd_chart.xAxis.axisMaximum = macd_chart.data.xMax + 0.5f
//        kdj_chart.xAxis.axisMaximum = kdj_chart.data.xMax + 0.5f


        //设置当前缩放程度
        for (i in 0..2) {
            initScale()
            //            macd_chart.zoom(MAX_COUNT * 1f / INIT_COUNT, 0f, 0f, 0f)
//            kdj_chart.zoom(MAX_COUNT * 1f / INIT_COUNT, 0f, 0f, 0f)

        }


        val hisData = lastData
        setDescription(vol_chart, "VOL " + DoubleUtil.amountConversion(hisData?.vol ?: 0.0, false))
        setMADescriptions(hisData?.ma5, hisData?.ma10, hisData?.ma20)
        kLineViewListener?.onMaChanged(hisData)
        if (price_chart.description.isEnabled) {
            setDescription(price_chart, String.format(Locale.getDefault(), "MA5:%.2f  MA10:%.2f  MA20:%.2f  MA30:%.2f", hisData?.ma5, hisData?.ma10, hisData?.ma20, hisData?.ma30))
        }
        if (macd_chart.description.isEnabled)
            setDescription(macd_chart, String.format(Locale.getDefault(), "MACD:%.2f  DEA:%.2f  DIF:%.2f",
                    hisData?.macd, hisData?.dea, hisData?.dif))
        if (kdj_chart.description.isEnabled)
            setDescription(kdj_chart, String.format(Locale.getDefault(), "K:%.2f  D:%.2f  J:%.2f",
                    hisData?.k, hisData?.d, hisData?.j))

        //为蒙版设置时间格式化
        k_info_mark.setDataFormatString(mDateFormat, isRedDown)

    }

    /**
     * 重新设置缩放
     */
    private fun initScale() {

        price_chart.setVisibleXRange(MAX_COUNT_FLAG.toFloat() * 2f, MIN_COUNT.toFloat())//设置可放大的最大程度
        vol_chart.setVisibleXRange(MAX_COUNT_FLAG.toFloat() * 2f, MIN_COUNT.toFloat())

        val currentScale = MAX_COUNT * 1f / INIT_COUNT
        val lastScale = price_chart.viewPortHandler.scaleX
        Logger.i("$lastScale  $currentScale")

        val toScale = currentScale / lastScale

        price_chart.zoom(toScale, 0f, 0f, 0f)
        vol_chart.zoom(toScale, 0f, 0f, 0f)
    }

    /**
     * 设置均线描述文字
     */
    private fun setMADescriptions(ma5: Double?, ma10: Double?, ma20: Double?) {

        tv_ma5.text = "MA5: ${DoubleUtil.amountConversion(ma5 ?: 0.0, false)}"
        tv_ma10.text = "MA10: ${DoubleUtil.amountConversion(ma10 ?: 0.0, false)}"
        tv_ma20.text = "MA20: ${DoubleUtil.amountConversion(ma20 ?: 0.0, false)}"
    }

    private fun initChartPriceData(): CombinedData {
        val lineCJEntries = ArrayList<CandleEntry>(INIT_COUNT)
        val ma5Entries = ArrayList<Entry>(INIT_COUNT)
        val ma10Entries = ArrayList<Entry>(INIT_COUNT)
        val ma20Entries = ArrayList<Entry>(INIT_COUNT)
        val ma30Entries = ArrayList<Entry>(INIT_COUNT)
        val paddingEntries = ArrayList<Entry>(INIT_COUNT)

        for (i in mData.indices) {
            val hisData = mData[i]
            lineCJEntries.add(CandleEntry(i.toFloat(), hisData.high?.toFloat() ?: 0f,
                    hisData.low?.toFloat() ?: 0f, hisData.open?.toFloat()
                    ?: 0f, hisData.close?.toFloat() ?: 0f))

            if (hisData.ma5?.isNaN() == false) {
                ma5Entries.add(Entry(i.toFloat(), hisData.ma5?.toFloat() ?: 0f))
            }

            if (hisData.ma10?.isNaN() == false) {
                ma10Entries.add(Entry(i.toFloat(), hisData.ma10?.toFloat() ?: 0f))
            }

            if (hisData.ma20?.isNaN() == false) {
                ma20Entries.add(Entry(i.toFloat(), hisData.ma20?.toFloat() ?: 0f))
            }

            if (hisData.ma30?.isNaN() == false) {
                ma30Entries.add(Entry(i.toFloat(), hisData.ma30?.toFloat() ?: 0f))
            }
        }

        if (!mData.isEmpty() && mData.size < MAX_COUNT) {
            (mData.size until MAX_COUNT).mapTo(paddingEntries) {
                Entry(it.toFloat(), mData[mData.size - 1].close?.toFloat() ?: 0f)
            }
        }

        val lineData = LineData(
                setLine(INVISIABLE_LINE, paddingEntries),
                setLine(MA5, ma5Entries),
                setLine(MA10, ma10Entries),
                setLine(MA20, ma20Entries)
                //                ,setLine(MA30, ma30Entries)
        )
        val candleData = CandleData(setKLine(NORMAL_LINE, lineCJEntries))
        val combinedData = CombinedData()
        combinedData.setData(lineData)
        combinedData.setData(candleData)
        price_chart.data = combinedData

        price_chart.notifyDataSetChanged()
        price_chart.xAxis.axisMaximum = price_chart.data.xMax + 0.5f
        price_chart.moveViewToX(mData.size - 1f)
//        moveToLast(price_chart)
        return combinedData
    }


    private fun setBar(barEntries: ArrayList<BarEntry>, type: Int): BarDataSet {
        val barDataSet = BarDataSet(barEntries, "vol")
        barDataSet.highLightAlpha = 150
        barDataSet.setHighlightLineWidth(resources.getDimension(R.dimen.highlight_width))
        barDataSet.highLightColor = resources.getColor(R.color.highlight_color)
        barDataSet.setDrawValues(false)
        barDataSet.isVisible = type != INVISIABLE_LINE
        barDataSet.isHighlightEnabled = type != INVISIABLE_LINE
        barDataSet.setColors(mIncreasingColor, mDecreasingColor)
        return barDataSet
    }


    private fun setLine(type: Int, lineEntries: ArrayList<Entry>): LineDataSet {
        val lineDataSetMa = LineDataSet(lineEntries, "ma$type")
        lineDataSetMa.setDrawValues(false)
        when (type) {
            NORMAL_LINE -> {
                lineDataSetMa.color = resources.getColor(R.color.normal_line_color)
                lineDataSetMa.setCircleColor(ContextCompat.getColor(mContext, R.color.normal_line_color))
            }
            K -> {
                lineDataSetMa.color = resources.getColor(R.color.k)
                lineDataSetMa.setCircleColor(mTransparentColor)
            }
            D -> {
                lineDataSetMa.color = resources.getColor(R.color.d)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            J -> {
                lineDataSetMa.color = resources.getColor(R.color.j)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            DIF -> {
                lineDataSetMa.color = resources.getColor(R.color.dif)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            DEA -> {
                lineDataSetMa.color = resources.getColor(R.color.dea)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            AVE_LINE -> {
                lineDataSetMa.color = resources.getColor(R.color.ave_color)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            MA5 -> {
                lineDataSetMa.color = resources.getColor(R.color.ma5)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            MA10 -> {
                lineDataSetMa.color = resources.getColor(R.color.ma10)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            MA20 -> {
                lineDataSetMa.color = resources.getColor(R.color.ma20)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            MA30 -> {
                lineDataSetMa.color = resources.getColor(R.color.ma30)
                lineDataSetMa.setCircleColor(mTransparentColor)
                lineDataSetMa.isHighlightEnabled = false
            }
            else -> {
                lineDataSetMa.isVisible = false
                lineDataSetMa.isHighlightEnabled = false
            }
        }
        lineDataSetMa.axisDependency = YAxis.AxisDependency.LEFT
        lineDataSetMa.lineWidth = 1f
        lineDataSetMa.circleRadius = 1f

        lineDataSetMa.setDrawCircles(false)
        lineDataSetMa.setDrawCircleHole(false)

        return lineDataSetMa
    }

    fun setKLine(type: Int, lineEntries: ArrayList<CandleEntry>): CandleDataSet {
        val set = CandleDataSet(lineEntries, "KLine$type")
        set.setDrawIcons(false)
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.shadowColor = Color.DKGRAY
        set.shadowWidth = 0.75f
        set.decreasingColor = mDecreasingColor
        set.decreasingPaintStyle = Paint.Style.FILL
        set.shadowColorSameAsCandle = true
        set.increasingColor = mIncreasingColor
        set.increasingPaintStyle = Paint.Style.FILL
        set.neutralColor = mIncreasingColor
        set.setDrawValues(true)
        set.valueTextSize = 10f
        set.isHighlightEnabled = true
        set.highLightColor = resources.getColor(R.color.highlight_color)
        set.highlightLineWidth = resources.getDimension(R.dimen.highlight_width)
        set.setmHighlightLineAlpha(150)

        if (type != NORMAL_LINE) {
            set.isVisible = false
        }
        return set
    }

    /**
     * 初始化交易额的图表
     */
    private fun initChartVolumeData(): CombinedData {
        val barEntries = ArrayList<BarEntry>()
        val ma5Entries = ArrayList<Entry>()
        val ma10Entries = ArrayList<Entry>()
        val paddingEntries = ArrayList<BarEntry>()

        for (i in mData.indices) {
            val hisData = mData[i]
            barEntries.add(BarEntry(i.toFloat(), hisData.vol?.toFloat() ?: 0f, hisData))
            if (hisData.volume_ma5?.isNaN() == false) {
                ma5Entries.add(Entry(i.toFloat(), hisData.volume_ma5?.toFloat() ?: 0f))
            }

            if (hisData.volume_ma10?.isNaN() == false) {
                ma10Entries.add(Entry(i.toFloat(), hisData.volume_ma10?.toFloat() ?: 0f))
            }
        }
        val maxCount = MAX_COUNT

        if (!mData.isEmpty() && mData.size < maxCount) {
            (mData.size until maxCount).mapTo(paddingEntries) { BarEntry(it.toFloat(), 0f) }
        }

        val barData = BarData(setBar(barEntries, NORMAL_LINE), setBar(paddingEntries, INVISIABLE_LINE))
        barData.barWidth = 0.75f

        val combinedData = CombinedData()
        combinedData.setData(barData)


        val lineData = LineData(setLine(MA5, ma5Entries), setLine(MA10, ma10Entries))
        combinedData.setData(lineData)

        vol_chart.data = combinedData
        vol_chart.notifyDataSetChanged()
        vol_chart.xAxis.axisMaximum = vol_chart.data.xMax + 0.5f
        vol_chart.moveViewToX(mData.size - 1f)
        return combinedData
    }

    private fun initChartMacdData() {
        val barEntries = ArrayList<BarEntry>()
        val paddingEntries = ArrayList<BarEntry>()
        val difEntries = ArrayList<Entry>()
        val deaEntries = ArrayList<Entry>()
        for (i in mData.indices) {
            val t = mData[i]
            barEntries.add(BarEntry(i.toFloat(), t.macd?.toFloat() ?: 0f))
            difEntries.add(Entry(i.toFloat(), t.dif?.toFloat() ?: 0f))
            deaEntries.add(Entry(i.toFloat(), t.dea?.toFloat() ?: 0f))
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
        val lineData = LineData(setLine(DIF, difEntries), setLine(DEA, deaEntries))
        combinedData.setData(lineData)
        macd_chart.data = combinedData

        macd_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        macd_chart.notifyDataSetChanged()
        moveToLast(macd_chart)
    }

    private fun initChartKdjData() {
        val kEntries = ArrayList<Entry>(INIT_COUNT)
        val dEntries = ArrayList<Entry>(INIT_COUNT)
        val jEntries = ArrayList<Entry>(INIT_COUNT)
        val paddingEntries = ArrayList<Entry>(INIT_COUNT)

        for (i in mData.indices) {
            kEntries.add(Entry(i.toFloat(), mData[i].k?.toFloat() ?: 0f))
            dEntries.add(Entry(i.toFloat(), mData[i].d?.toFloat() ?: 0f))
            jEntries.add(Entry(i.toFloat(), mData[i].j?.toFloat() ?: 0f))
        }
        if (!mData.isEmpty() && mData.size < MAX_COUNT) {
            (mData.size until MAX_COUNT).mapTo(paddingEntries) {
                Entry(it.toFloat(), mData[mData.size - 1].k?.toFloat() ?: 0f)
            }
        }
        val sets = ArrayList<ILineDataSet>()
        sets.add(setLine(K, kEntries))
        sets.add(setLine(D, dEntries))
        sets.add(setLine(J, jEntries))
        sets.add(setLine(INVISIABLE_LINE, paddingEntries))
        val lineData = LineData(sets)

        val combinedData = CombinedData()
        combinedData.setData(lineData)
        kdj_chart.data = combinedData

        macd_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        kdj_chart.notifyDataSetChanged()
        moveToLast(kdj_chart)
    }


    /**
     * according to the price to refresh the last data of the chart
     */
    fun refreshData(price: Double) {
        if (price <= 0 || price == mLastPrice) {
            return
        }
        mLastPrice = price
        val data = price_chart.data ?: return
        val lineData = data.lineData
        if (lineData != null) {
            val set = lineData.getDataSetByIndex(0)
            if (set.removeLast()) {
                set.addEntry(Entry(set.entryCount.toFloat(), price.toFloat()))
            }
        }
        val candleData = data.candleData
        if (candleData != null) {
            val set = candleData.getDataSetByIndex(0)
            if (set.removeLast()) {
                val hisData = mData[mData.size - 1]
                hisData.close = price
                hisData.high = Math.max(hisData.high ?: 0.0, price)
                hisData.low = Math.min(hisData.low ?: 0.0, price)
                set.addEntry(CandleEntry(set.entryCount.toFloat(), hisData.high?.toFloat() ?: 0f,
                        hisData.low?.toFloat() ?: 0f, hisData.open?.toFloat()
                        ?: 0f, price.toFloat()))

            }
        }
        price_chart.notifyDataSetChanged()
        price_chart.invalidate()
    }

    fun addDatas(hisDatas: List<HisData>) {
        for (hisData in hisDatas) {
            addData(hisData)
        }
    }

    fun addData(hisData: HisData) {
        var hisData = hisData
        hisData = DataUtils.calculateHisData(hisData, mData)
        val combinedData = price_chart.data
        val priceData = combinedData.lineData
        val padding = priceData.getDataSetByIndex(0)
        val ma5Set = priceData.getDataSetByIndex(1)
        val ma10Set = priceData.getDataSetByIndex(2)
        val ma20Set = priceData.getDataSetByIndex(3)
        val ma30Set = priceData.getDataSetByIndex(4)
        val kData = combinedData.candleData
        val klineSet = kData.getDataSetByIndex(0)
        val volSet = vol_chart.data.barData.getDataSetByIndex(0)
        val macdSet = macd_chart.data.barData.getDataSetByIndex(0)
        val difSet = macd_chart.data.lineData.getDataSetByIndex(0)
        val deaSet = macd_chart.data.lineData.getDataSetByIndex(1)
        val kdjData = kdj_chart.data.lineData
        val kSet = kdjData.getDataSetByIndex(0)
        val dSet = kdjData.getDataSetByIndex(1)
        val jSet = kdjData.getDataSetByIndex(2)

        if (mData.contains(hisData)) {
            val index = mData.indexOf(hisData)
            klineSet.removeEntry(index)
            padding.removeFirst()
            // ma比较特殊，entry数量和k线的不一致，移除最后一个
            ma5Set.removeLast()
            ma10Set.removeLast()
            ma20Set.removeLast()
            ma30Set.removeLast()
            volSet.removeEntry(index)
            macdSet.removeEntry(index)
            difSet.removeEntry(index)
            deaSet.removeEntry(index)
            kSet.removeEntry(index)
            dSet.removeEntry(index)
            jSet.removeEntry(index)
            mData.removeAt(index)

        }
        mData.add(hisData)
        price_chart.realCount = mData.size
        val klineCount = klineSet.entryCount
        klineSet.addEntry(CandleEntry(klineCount.toFloat(), hisData.high?.toFloat() ?: 0f,
                hisData.low?.toFloat() ?: 0f, hisData.open?.toFloat() ?: 0f,
                hisData.close?.toFloat() ?: 0f))
        volSet.addEntry(BarEntry(volSet.entryCount.toFloat(), hisData.vol?.toFloat()
                ?: 0f, hisData))

        macdSet.addEntry(BarEntry(macdSet.entryCount.toFloat(), hisData.macd?.toFloat() ?: 0f))
        difSet.addEntry(Entry(difSet.entryCount.toFloat(), hisData.dif?.toFloat() ?: 0f))
        deaSet.addEntry(Entry(deaSet.entryCount.toFloat(), hisData.dea?.toFloat() ?: 0f))

        kSet.addEntry(Entry(kSet.entryCount.toFloat(), hisData.k?.toFloat() ?: 0f))
        dSet.addEntry(Entry(dSet.entryCount.toFloat(), hisData.d?.toFloat() ?: 0f))
        jSet.addEntry(Entry(jSet.entryCount.toFloat(), hisData.j?.toFloat() ?: 0f))

        // 因为ma的数量会少，所以这里用kline的set数量作为x

        if (hisData.ma5?.isNaN() == false) {
            ma5Set.addEntry(Entry(klineCount.toFloat(), hisData.ma5?.toFloat() ?: 0f))
        }
        if (hisData.ma10?.isNaN() == false) {
            ma10Set.addEntry(Entry(klineCount.toFloat(), hisData.ma10?.toFloat() ?: 0f))
        }
        if (hisData.ma20?.isNaN() == false) {
            ma20Set.addEntry(Entry(klineCount.toFloat(), hisData.ma20?.toFloat() ?: 0f))
        }
        if (hisData.ma30?.isNaN() == false) {
            ma30Set.addEntry(Entry(klineCount.toFloat(), hisData.ma30?.toFloat() ?: 0f))
        }


        price_chart.xAxis.axisMaximum = combinedData.xMax + 1.5f
        vol_chart.xAxis.axisMaximum = vol_chart.data.xMax + 1.5f
        macd_chart.xAxis.axisMaximum = macd_chart.data.xMax + 1.5f
        kdj_chart.xAxis.axisMaximum = kdj_chart.data.xMax + 1.5f


        price_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        vol_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        macd_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        kdj_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        price_chart.notifyDataSetChanged()
        price_chart.invalidate()
        vol_chart.notifyDataSetChanged()
        vol_chart.invalidate()
        macd_chart.notifyDataSetChanged()
        macd_chart.invalidate()
        kdj_chart.notifyDataSetChanged()
        kdj_chart.invalidate()


        setChartDescription(hisData)

    }

    fun addDatasFirst(hisDatas: List<HisData>) {
        val combinedData = price_chart.data
        val priceData = combinedData.lineData
        val padding = priceData.getDataSetByIndex(0)
        val ma5Set = priceData.getDataSetByIndex(1)
        val ma10Set = priceData.getDataSetByIndex(2)
        val ma20Set = priceData.getDataSetByIndex(3)
        val ma30Set = priceData.getDataSetByIndex(4)
        val kData = combinedData.candleData
        val klineSet = kData.getDataSetByIndex(0)
        val volSet = vol_chart.data.barData.getDataSetByIndex(0)
        val macdSet = macd_chart.data.barData.getDataSetByIndex(0)
        val difSet = macd_chart.data.lineData.getDataSetByIndex(0)
        val deaSet = macd_chart.data.lineData.getDataSetByIndex(1)
        val kdjData = kdj_chart.data.lineData
        val kSet = kdjData.getDataSetByIndex(0)
        val dSet = kdjData.getDataSetByIndex(1)
        val jSet = kdjData.getDataSetByIndex(2)

        mData.addAll(0, hisDatas)
        // 这里需要重新绘制图表，把之前的图表清理掉
        klineSet.clear()
        padding.clear()
        ma5Set.clear()
        ma10Set.clear()
        ma20Set.clear()
        ma30Set.clear()
        volSet.clear()
        macdSet.clear()
        difSet.clear()
        deaSet.clear()
        deaSet.clear()
        kSet.clear()
        dSet.clear()
        jSet.clear()

        // 重新计算各个指标
        DataUtils.calculateHisData(mData)
        price_chart.realCount = mData.size
        for (i in mData.indices) {
            val hisData = mData[i]
            klineSet.addEntry(CandleEntry(i.toFloat(), hisData.high?.toFloat() ?: 0f,
                    hisData.low?.toFloat() ?: 0f, hisData.open?.toFloat() ?: 0f,
                    hisData.close?.toFloat() ?: 0f))

            if (hisData.ma5?.isNaN() == false) {
                ma5Set.addEntry(Entry(i.toFloat(), hisData.ma5?.toFloat() ?: 0f))
            }

            if (hisData.ma10?.isNaN() == false) {
                ma10Set.addEntry(Entry(i.toFloat(), hisData.ma10?.toFloat() ?: 0f))
            }

            if (hisData.ma20?.isNaN() == false) {
                ma20Set.addEntry(Entry(i.toFloat(), hisData.ma20?.toFloat() ?: 0f))
            }

            if (hisData.ma30?.isNaN() == false) {
                ma30Set.addEntry(Entry(i.toFloat(), hisData.ma30?.toFloat() ?: 0f))
            }
            volSet.addEntry(BarEntry(i.toFloat(), hisData.vol?.toFloat() ?: 0f, hisData))

            macdSet.addEntry(BarEntry(i.toFloat(), hisData.macd?.toFloat() ?: 0f))
            difSet.addEntry(Entry(i.toFloat(), hisData.dif?.toFloat() ?: 0f))
            deaSet.addEntry(Entry(i.toFloat(), hisData.dea?.toFloat() ?: 0f))

            kSet.addEntry(Entry(i.toFloat(), hisData.k?.toFloat() ?: 0f))
            dSet.addEntry(Entry(i.toFloat(), hisData.d?.toFloat() ?: 0f))
            jSet.addEntry(Entry(i.toFloat(), hisData.j?.toFloat() ?: 0f))
        }


        price_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        vol_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        macd_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())
        kdj_chart.setVisibleXRange(MAX_COUNT.toFloat(), MIN_COUNT.toFloat())

        price_chart.moveViewToX(hisDatas.size - 0.5f)
        vol_chart.moveViewToX(hisDatas.size - 0.5f)
        macd_chart.moveViewToX(hisDatas.size - 0.5f)
        kdj_chart.moveViewToX(hisDatas.size - 0.5f)

        price_chart.notifyDataSetChanged()
        price_chart.invalidate()
        vol_chart.notifyDataSetChanged()
        vol_chart.invalidate()
        macd_chart.notifyDataSetChanged()
        macd_chart.invalidate()
        kdj_chart.notifyDataSetChanged()
        kdj_chart.invalidate()


        val hisData = mData[0]
        setChartDescription(hisData)

    }

    private fun setChartDescription(hisData: HisData) {
        kLineViewListener?.onMaChanged(hisData)
        if (price_chart.description.isEnabled)
            setDescription(price_chart, String.format(Locale.getDefault(), "MA5:%.2f  MA10:%.2f  MA20:%.2f  MA30:%.2f",
                    hisData.ma5, hisData.ma10, hisData.ma20, hisData.ma30))
        setDescription(vol_chart, "VOL " + DoubleUtil.amountConversion(hisData.vol ?: 0.0, false))
        setMADescriptions(hisData.ma5, hisData.ma10, hisData.ma20)
        if (macd_chart.description.isEnabled)
            setDescription(macd_chart, String.format(Locale.getDefault(), "MACD:%.2f  DEA:%.2f  DIF:%.2f",
                    hisData.macd, hisData.dea, hisData.dif))
        if (kdj_chart.description.isEnabled)
            setDescription(kdj_chart, String.format(Locale.getDefault(), "K:%.2f  D:%.2f  J:%.2f",
                    hisData.k, hisData.d, hisData.j))
    }


    /**
     * align two chart
     */
    private fun setOffset() {
        val chartHeight = resources.getDimensionPixelSize(R.dimen.bottom_chart_height) +
                DisplayUtils.dip2px(mContext, 20f)
        price_chart.setViewPortOffsets(0f, 0f, 0f, chartHeight.toFloat())
        val bottom = DisplayUtils.dip2px(mContext, 5f)
        vol_chart.setViewPortOffsets(0f, 0f, 0f, bottom.toFloat())
        macd_chart.setViewPortOffsets(0f, 0f, 0f, bottom.toFloat())
        kdj_chart.setViewPortOffsets(0f, 0f, 0f, bottom.toFloat())
    }


    /**
     * add limit line to chart
     */
    @JvmOverloads
    fun setLimitLine(lastClose: Double = mLastClose) {
        val limitLine = LimitLine(lastClose.toFloat())
        limitLine.enableDashedLine(5f, 10f, 0f)
        limitLine.lineColor = resources.getColor(R.color.limit_color)
        price_chart.axisLeft.addLimitLine(limitLine)
    }

    fun setLastClose(lastClose: Double) {
        mLastClose = lastClose
        price_chart.setOnChartValueSelectedListener(InfoViewListener(this, mContext, mLastClose, mData, k_info_mark, vol_chart, macd_chart, kdj_chart))
        vol_chart.setOnChartValueSelectedListener(InfoViewListener(this, mContext, mLastClose, mData, k_info_mark, price_chart, macd_chart, kdj_chart))
        macd_chart.setOnChartValueSelectedListener(InfoViewListener(this, mContext, mLastClose, mData, k_info_mark, price_chart, vol_chart, kdj_chart))
        kdj_chart.setOnChartValueSelectedListener(InfoViewListener(this, mContext, mLastClose, mData, k_info_mark, price_chart, vol_chart, macd_chart))

    }


    override fun onAxisChange(chart: BarLineChartBase<*>) {
        val lowestVisibleX = chart.lowestVisibleX
        if (lowestVisibleX <= chart.xAxis.axisMinimum) return
        val maxX = chart.highestVisibleX.toInt()
        val x = Math.min(maxX, mData.size - 1)
        val hisData = mData[if (x < 0) 0 else x]
        setChartDescription(hisData)
        k_info_mark.closeHightLight()
    }

    override fun onAxisTranslate(chart: BarLineChartBase<*>) {
//        k_info_mark.translate(price_chart.,dY)
    }

    fun setOnLoadMoreListener(l: OnLoadMoreListener) {
        if (mCoupleChartGestureListener != null) {
            mCoupleChartGestureListener?.setOnLoadMoreListener(l)
        }
    }

    fun loadMoreComplete() {
        if (mCoupleChartGestureListener != null) {
            mCoupleChartGestureListener?.loadMoreComplete()
        }
    }

    companion object {


        val NORMAL_LINE = 0
        /**
         * average line
         */
        val AVE_LINE = 1
        /**
         * hide line
         */
        val INVISIABLE_LINE = 6


        val MA5 = 5
        val MA10 = 10
        val MA20 = 20
        val MA30 = 30

        val K = 31
        val D = 32
        val J = 33

        val DIF = 34
        val DEA = 35
    }

    /**
     *
     */
    interface KLineViewListener {
        fun onMaChanged(hisData: HisData?)
    }

    fun updateValueSelected(hisData: HisData) {
        setDescription(vol_chart, "VOL " + DoubleUtil.amountConversion(hisData.vol ?: 0.0, false))
        kLineViewListener?.onMaChanged(hisData)
        setMADescriptions(hisData.ma5, hisData.ma10, hisData.ma20)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        initScale()
    }
}
