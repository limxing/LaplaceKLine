package cn.laplacetech.klinelib.chart

import android.graphics.Canvas
import android.graphics.Paint

import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.CandleDataProvider
import com.github.mikephil.charting.interfaces.datasets.ICandleDataSet
import com.github.mikephil.charting.renderer.LineScatterCandleRadarRenderer
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class CandleStickChartRenderer(var mChart: CandleDataProvider, animator: ChartAnimator,
                               viewPortHandler: ViewPortHandler) : LineScatterCandleRadarRenderer(animator, viewPortHandler) {

    private val mShadowBuffers = FloatArray(8)
    private val mBodyBuffers = FloatArray(4)
    private val mRangeBuffers = FloatArray(4)
    private val mOpenBuffers = FloatArray(4)
    private val mCloseBuffers = FloatArray(4)

    override fun initBuffers() {

    }

    override fun drawData(c: Canvas) {

        val candleData = mChart.candleData

        for (set in candleData.dataSets) {

            if (set.isVisible)
                drawDataSet(c, set)
        }
    }

    protected fun drawDataSet(c: Canvas, dataSet: ICandleDataSet) {

        val trans = mChart.getTransformer(dataSet.axisDependency)

        val phaseY = mAnimator.phaseY
        val barSpace = dataSet.barSpace
        val showCandleBar = dataSet.showCandleBar

        mXBounds.set(mChart, dataSet)

        mRenderPaint.strokeWidth = dataSet.shadowWidth

        // draw the body
        for (j in mXBounds.min..mXBounds.range + mXBounds.min) {

            // get the entry
            val e = dataSet.getEntryForIndex(j) ?: continue

            val xPos = e.x

            val open = e.open
            val close = e.close
            val high = e.high
            val low = e.low

            if (showCandleBar) {
                // calculate the shadow

                mShadowBuffers[0] = xPos
                mShadowBuffers[2] = xPos
                mShadowBuffers[4] = xPos
                mShadowBuffers[6] = xPos

                if (open > close) {
                    mShadowBuffers[1] = high * phaseY
                    mShadowBuffers[3] = open * phaseY
                    mShadowBuffers[5] = low * phaseY
                    mShadowBuffers[7] = close * phaseY
                } else if (open < close) {
                    mShadowBuffers[1] = high * phaseY
                    mShadowBuffers[3] = close * phaseY
                    mShadowBuffers[5] = low * phaseY
                    mShadowBuffers[7] = open * phaseY
                } else {
                    mShadowBuffers[1] = high * phaseY
                    mShadowBuffers[3] = open * phaseY
                    mShadowBuffers[5] = low * phaseY
                    mShadowBuffers[7] = mShadowBuffers[3]
                }

                trans.pointValuesToPixel(mShadowBuffers)

                // draw the shadows

                if (dataSet.shadowColorSameAsCandle) {

                    if (open > close)
                        mRenderPaint.color = if (dataSet.decreasingColor == ColorTemplate.COLOR_NONE)
                            dataSet.getColor(j)
                        else
                            dataSet.decreasingColor
                    else if (open < close)
                        mRenderPaint.color = if (dataSet.increasingColor == ColorTemplate.COLOR_NONE)
                            dataSet.getColor(j)
                        else
                            dataSet.increasingColor
                    else
                        mRenderPaint.color = if (dataSet.neutralColor == ColorTemplate.COLOR_NONE)
                            dataSet.getColor(j)
                        else
                            dataSet.neutralColor

                } else {
                    mRenderPaint.color = if (dataSet.shadowColor == ColorTemplate.COLOR_NONE)
                        dataSet.getColor(j)
                    else
                        dataSet.shadowColor
                }

                mRenderPaint.style = Paint.Style.STROKE

                c.drawLines(mShadowBuffers, mRenderPaint)

                // calculate the body

                mBodyBuffers[0] = xPos - 0.5f + barSpace
                mBodyBuffers[1] = close * phaseY
                mBodyBuffers[2] = xPos + 0.5f - barSpace
                mBodyBuffers[3] = open * phaseY

                trans.pointValuesToPixel(mBodyBuffers)

                // draw body differently for increasing and decreasing entry
                if (open > close) { // decreasing

                    if (dataSet.decreasingColor == ColorTemplate.COLOR_NONE) {
                        mRenderPaint.color = dataSet.getColor(j)
                    } else {
                        mRenderPaint.color = dataSet.decreasingColor
                    }

                    mRenderPaint.style = dataSet.decreasingPaintStyle

                    c.drawRect(
                            mBodyBuffers[0], mBodyBuffers[3],
                            mBodyBuffers[2], mBodyBuffers[1],
                            mRenderPaint)

                } else if (open < close) {

                    if (dataSet.increasingColor == ColorTemplate.COLOR_NONE) {
                        mRenderPaint.color = dataSet.getColor(j)
                    } else {
                        mRenderPaint.color = dataSet.increasingColor
                    }

                    mRenderPaint.style = dataSet.increasingPaintStyle

                    c.drawRect(
                            mBodyBuffers[0], mBodyBuffers[1],
                            mBodyBuffers[2], mBodyBuffers[3],
                            mRenderPaint)
                } else { // equal values

                    if (dataSet.neutralColor == ColorTemplate.COLOR_NONE) {
                        mRenderPaint.color = dataSet.getColor(j)
                    } else {
                        mRenderPaint.color = dataSet.neutralColor
                    }

                    c.drawLine(
                            mBodyBuffers[0], mBodyBuffers[1],
                            mBodyBuffers[2], mBodyBuffers[3],
                            mRenderPaint)
                }
            } else {

                mRangeBuffers[0] = xPos
                mRangeBuffers[1] = high * phaseY
                mRangeBuffers[2] = xPos
                mRangeBuffers[3] = low * phaseY

                mOpenBuffers[0] = xPos - 0.5f + barSpace
                mOpenBuffers[1] = open * phaseY
                mOpenBuffers[2] = xPos
                mOpenBuffers[3] = open * phaseY

                mCloseBuffers[0] = xPos + 0.5f - barSpace
                mCloseBuffers[1] = close * phaseY
                mCloseBuffers[2] = xPos
                mCloseBuffers[3] = close * phaseY

                trans.pointValuesToPixel(mRangeBuffers)
                trans.pointValuesToPixel(mOpenBuffers)
                trans.pointValuesToPixel(mCloseBuffers)

                // draw the ranges
                val barColor: Int

                if (open > close)
                    barColor = if (dataSet.decreasingColor == ColorTemplate.COLOR_NONE)
                        dataSet.getColor(j)
                    else
                        dataSet.decreasingColor
                else if (open < close)
                    barColor = if (dataSet.increasingColor == ColorTemplate.COLOR_NONE)
                        dataSet.getColor(j)
                    else
                        dataSet.increasingColor
                else
                    barColor = if (dataSet.neutralColor == ColorTemplate.COLOR_NONE)
                        dataSet.getColor(j)
                    else
                        dataSet.neutralColor

                mRenderPaint.color = barColor
                c.drawLine(
                        mRangeBuffers[0], mRangeBuffers[1],
                        mRangeBuffers[2], mRangeBuffers[3],
                        mRenderPaint)
                c.drawLine(
                        mOpenBuffers[0], mOpenBuffers[1],
                        mOpenBuffers[2], mOpenBuffers[3],
                        mRenderPaint)
                c.drawLine(
                        mCloseBuffers[0], mCloseBuffers[1],
                        mCloseBuffers[2], mCloseBuffers[3],
                        mRenderPaint)
            }
        }
    }

    /*@Override
    public void drawValues(Canvas c) {

        // if values are drawn
        if (isDrawingValuesAllowed(mChart)) {

            List<ICandleDataSet> dataSets = mChart.getCandleData().getDataSets();

            for (int i = 0; i < dataSets.size(); i++) {

                ICandleDataSet dataSet = dataSets.get(i);

                if (!shouldDrawValues(dataSet))
                    continue;

                // apply the text-styling defined by the DataSet
                applyValueTextStyle(dataSet);

                Transformer trans = mChart.getTransformer(dataSet.getAxisDependency());

                mXBounds.set(mChart, dataSet);

                float[] positions = trans.generateTransformedValuesCandle(
                        dataSet, mAnimator.getPhaseX(), mAnimator.getPhaseY(), mXBounds.min, mXBounds.max);

                float yOffset = Utils.convertDpToPixel(5f);

                MPPointF iconsOffset = MPPointF.getInstance(dataSet.getIconsOffset());
                iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x);
                iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y);

                for (int j = 0; j < positions.length; j += 2) {

                    float x = positions[j];
                    float y = positions[j + 1];

                    if (!mViewPortHandler.isInBoundsRight(x))
                        break;

                    if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y))
                        continue;

                    CandleEntry entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min);

                    if (dataSet.isDrawValuesEnabled()) {
                        drawValue(c,
                                dataSet.getValueFormatter(),
                                entry.getHigh(),
                                entry,
                                i,
                                x,
                                y - yOffset,
                                dataSet
                                        .getValueTextColor(j / 2));
                    }

                    if (entry.getIcon() != null && dataSet.isDrawIconsEnabled()) {

                        Drawable icon = entry.getIcon();

                        Utils.drawImage(
                                c,
                                icon,
                                (int)(x + iconsOffset.x),
                                (int)(y + iconsOffset.y),
                                icon.getIntrinsicWidth(),
                                icon.getIntrinsicHeight());
                    }
                }

                MPPointF.recycleInstance(iconsOffset);
            }


        }


    }*/

    override fun drawValues(c: Canvas) {

        val dataSets = mChart.candleData.dataSets

        for (i in dataSets.indices) {

            val dataSet = dataSets[i]

            if (!dataSet.isDrawValuesEnabled || dataSet.entryCount == 0)
                continue

            // apply the text-styling defined by the DataSet
            applyValueTextStyle(dataSet)

            val trans = mChart.getTransformer(dataSet.axisDependency)

            val minx = Math.max(dataSet.xMin, 0f).toInt()
            val maxx = Math.min(dataSet.xMax, (dataSet.entryCount - 1).toFloat()).toInt()

            val positions = trans.generateTransformedValuesCandle(
                    dataSet, mAnimator.phaseX, mAnimator.phaseY, minx, maxx)


            //计算最大值和最小值
            var maxValue = 0f
            var minValue = 0f
            var maxIndex = 0
            var minIndex = 0
            var maxEntry: CandleEntry? = null
            var firstInit = true
            var j = 0
            while (j < positions.size) {

                val x = positions[j]
                val y = positions[j + 1]

                if (!mViewPortHandler.isInBoundsRight(x))
                    break

                if (!mViewPortHandler.isInBoundsLeft(x) || !mViewPortHandler.isInBoundsY(y)) {
                    j += 2
                    continue
                }

                val entry = dataSet.getEntryForIndex(j / 2 + minx)

                if (firstInit) {
                    maxValue = entry.high
                    minValue = entry.low
                    firstInit = false
                    maxEntry = entry
                } else {
                    if (entry.high > maxValue) {
                        maxValue = entry.high
                        maxIndex = j
                        maxEntry = entry
                    }

                    if (entry.low < minValue) {
                        minValue = entry.low
                        minIndex = j
                    }

                }
                j += 2
            }

            //绘制最大值和最小值
            val x = positions[minIndex]
            if (maxIndex > minIndex) {
                //画右边
                val highString = "← " + java.lang.Float.toString(minValue)

                //计算显示位置
                //计算文本宽度
                val highStringWidth = Utils.calcTextWidth(mValuePaint, highString)

                val tPosition = FloatArray(2)
                tPosition[1] = minValue
                trans.pointValuesToPixel(tPosition)
                mValuePaint.color = dataSet.getValueTextColor(minIndex / 2)
                c.drawText(highString, x + highStringWidth / 2, tPosition[1], mValuePaint)
            } else {
                //画左边
                val highString = java.lang.Float.toString(minValue) + " →"

                //计算显示位置
                val highStringWidth = Utils.calcTextWidth(mValuePaint, highString)
                val tPosition = FloatArray(2)
                tPosition[1] = minValue
                trans.pointValuesToPixel(tPosition)
                mValuePaint.color = dataSet.getValueTextColor(minIndex / 2)
                c.drawText(highString, x - highStringWidth / 2, tPosition[1], mValuePaint)
            }

            if (maxIndex > minIndex) {
                //画左边
                val highString = java.lang.Float.toString(maxValue) + " →"

                val highStringWidth = Utils.calcTextWidth(mValuePaint, highString)

                val tPosition = FloatArray(2)
                tPosition[0] = if (maxEntry == null) 0f else maxEntry.x
                tPosition[1] = if (maxEntry == null) 0f else maxEntry.high
                trans.pointValuesToPixel(tPosition)

                mValuePaint.color = dataSet.getValueTextColor(maxIndex / 2)
                c.drawText(highString, tPosition[0] - highStringWidth / 2, tPosition[1], mValuePaint)
            } else {
                //画右边
                val highString = "← " + java.lang.Float.toString(maxValue)

                //计算显示位置
                val highStringWidth = Utils.calcTextWidth(mValuePaint, highString)

                val tPosition = FloatArray(2)
                tPosition[0] = if (maxEntry == null) 0f else maxEntry.x
                tPosition[1] = if (maxEntry == null) 0f else maxEntry.high
                trans.pointValuesToPixel(tPosition)

                mValuePaint.color = dataSet.getValueTextColor(maxIndex / 2)
                c.drawText(highString, tPosition[0] + highStringWidth / 2, tPosition[1], mValuePaint)

            }

        }
        //        }
    }


    override fun drawExtras(c: Canvas) {}

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {

        val candleData = mChart.candleData

        for (high in indices) {

            val set = candleData.getDataSetByIndex(high.dataSetIndex)

            if (set == null || !set.isHighlightEnabled)
                continue

            val e = set.getEntryForXValue(high.x, high.y)

            if (!isInBoundsX(e, set))
                continue

            val lowValue = e.low * mAnimator.phaseY
            val highValue = e.high * mAnimator.phaseY
            val y = (lowValue + highValue) / 2f

            val pix = mChart.getTransformer(set.axisDependency).getPixelForValues(e.x, y)

            high.setDraw(pix.x.toFloat(), pix.y.toFloat())

            // draw the lines
            drawHighlightLines(c, pix.x.toFloat(), pix.y.toFloat(), set)
        }
    }
}
