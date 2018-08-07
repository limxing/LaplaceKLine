package cn.laplacetech.klinelib.chart

import android.graphics.Canvas

import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.CombinedChart.DrawOrder
import com.github.mikephil.charting.data.ChartData
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.renderer.DataRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

import java.lang.ref.WeakReference
import java.util.ArrayList

/**
 * Renderer class that is responsible for rendering multiple different data-types.
 */
class CustomCombinedChartRenderer(chart: CombinedChart, animator: ChartAnimator, viewPortHandler: ViewPortHandler) : DataRenderer(animator, viewPortHandler) {

    /**
     * all rederers for the different kinds of data this combined-renderer can draw
     */
    protected var mRenderers: MutableList<DataRenderer> = ArrayList(5)

    protected var mChart: WeakReference<Chart<*>>

    protected var mHighlightBuffer: MutableList<Highlight> = ArrayList()

    /**
     * Returns all sub-renderers.
     *
     */
    var subRenderers: MutableList<DataRenderer>
        get() = mRenderers
        set(renderers) {
            this.mRenderers = renderers
        }

    init {
        mChart = WeakReference(chart)
        createRenderers()
    }

    /**
     * Creates the renderers needed for this combined-renderer in the required order. Also takes the DrawOrder into
     * consideration.
     */
    fun createRenderers() {

        mRenderers.clear()

        val chart = mChart.get() as CombinedChart

        val orders = chart.drawOrder

        for (order in orders) {

            when (order) {
                CombinedChart.DrawOrder.BAR -> if (chart.barData != null)
                    mRenderers.add(BarChartRenderer(chart, mAnimator, mViewPortHandler))
                CombinedChart.DrawOrder.BUBBLE -> if (chart.bubbleData != null)
                    mRenderers.add(BubbleChartRenderer(chart, mAnimator, mViewPortHandler))
                CombinedChart.DrawOrder.LINE -> if (chart.lineData != null)
                    mRenderers.add(CustomLineChartRenderer(chart, mAnimator, mViewPortHandler))
                CombinedChart.DrawOrder.CANDLE -> if (chart.candleData != null)
                    mRenderers.add(CandleStickChartRenderer(chart, mAnimator, mViewPortHandler))
                CombinedChart.DrawOrder.SCATTER -> if (chart.scatterData != null)
                    mRenderers.add(ScatterChartRenderer(chart, mAnimator, mViewPortHandler))
            }
        }
    }

    override fun initBuffers() {

        for (renderer in mRenderers)
            renderer.initBuffers()
    }

    override fun drawData(c: Canvas) {

        for (renderer in mRenderers)
            renderer.drawData(c)
    }

    override fun drawValues(c: Canvas) {

        for (renderer in mRenderers)
            renderer.drawValues(c)
    }

    override fun drawExtras(c: Canvas) {

        for (renderer in mRenderers)
            renderer.drawExtras(c)
    }

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {

        val chart = mChart.get() ?: return

        for (renderer in mRenderers) {
            var data: ChartData<*>? = null

            if (renderer is BarChartRenderer)
                data = renderer.mChart.barData
            else if (renderer is CustomLineChartRenderer)
                data = renderer.mChart.lineData
            else if (renderer is CandleStickChartRenderer)
                data = renderer.mChart.candleData
            else if (renderer is ScatterChartRenderer)
                data = renderer.mChart.scatterData
            else if (renderer is BubbleChartRenderer)
                data = renderer.mChart.bubbleData

            val dataIndex = if (data == null)
                -1
            else
                (chart.data as CombinedData).allData.indexOf(data)

            mHighlightBuffer.clear()

            for (h in indices) {
                if (h.dataIndex == dataIndex || h.dataIndex == -1)
                    mHighlightBuffer.add(h)
            }

            renderer.drawHighlighted(c, mHighlightBuffer.toTypedArray<Highlight>())
        }
    }

    /**
     * Returns the sub-renderer object at the specified index.
     *
     */
    fun getSubRenderer(index: Int): DataRenderer? {
        return if (index >= mRenderers.size || index < 0)
            null
        else
            mRenderers[index]
    }
}
