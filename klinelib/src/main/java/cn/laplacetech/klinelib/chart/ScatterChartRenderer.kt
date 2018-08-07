package cn.laplacetech.klinelib.chart

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Log

import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.ScatterData
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.ScatterDataProvider
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet
import com.github.mikephil.charting.renderer.LineScatterCandleRadarRenderer
import com.github.mikephil.charting.renderer.scatter.IShapeRenderer
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class ScatterChartRenderer(var mChart: ScatterDataProvider, animator: ChartAnimator, viewPortHandler: ViewPortHandler) : LineScatterCandleRadarRenderer(animator, viewPortHandler) {

    internal var mPixelBuffer = FloatArray(2)

    override fun initBuffers() {}

    override fun drawData(c: Canvas) {

        val scatterData = mChart.scatterData

        for (set in scatterData.dataSets) {

            if (set.isVisible)
                drawDataSet(c, set)
        }
    }

    protected fun drawDataSet(c: Canvas, dataSet: IScatterDataSet) {

        val viewPortHandler = mViewPortHandler

        val trans = mChart.getTransformer(dataSet.axisDependency)

        val phaseY = mAnimator.phaseY

        val renderer = dataSet.shapeRenderer
        if (renderer == null) {
            Log.i("MISSING", "There's no IShapeRenderer specified for ScatterDataSet")
            return
        }

        val max = Math.min(
                Math.ceil((dataSet.entryCount.toFloat() * mAnimator.phaseX).toDouble()),
                dataSet.entryCount.toFloat().toDouble()).toInt()

        for (i in 0 until max) {

            val e = dataSet.getEntryForIndex(i)

            mPixelBuffer[0] = e.x
            mPixelBuffer[1] = e.y * phaseY

            trans.pointValuesToPixel(mPixelBuffer)

            if (!viewPortHandler.isInBoundsRight(mPixelBuffer[0]))
                break

            if (!viewPortHandler.isInBoundsLeft(mPixelBuffer[0]) || !viewPortHandler.isInBoundsY(mPixelBuffer[1]))
                continue

            mRenderPaint.color = dataSet.getColor(i / 2)
            renderer.renderShape(
                    c, dataSet, mViewPortHandler,
                    mPixelBuffer[0], mPixelBuffer[1],
                    mRenderPaint)
        }
    }

    override fun drawValues(c: Canvas) {

        // if values are drawn
        if (isDrawingValuesAllowed(mChart)) {

            val dataSets = mChart.scatterData.dataSets

            for (i in 0 until mChart.scatterData.dataSetCount) {

                val dataSet = dataSets[i]

                if (!shouldDrawValues(dataSet))
                    continue

                // apply the text-styling defined by the DataSet
                applyValueTextStyle(dataSet)

                mXBounds.set(mChart, dataSet)

                val positions = mChart.getTransformer(dataSet.axisDependency)
                        .generateTransformedValuesScatter(dataSet,
                                mAnimator.phaseX, mAnimator.phaseY, mXBounds.min, mXBounds.max)

                val shapeSize = Utils.convertDpToPixel(dataSet.scatterShapeSize)

                val iconsOffset = MPPointF.getInstance(dataSet.iconsOffset)
                iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x)
                iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y)

                var j = 0
                while (j < positions.size) {

                    if (!mViewPortHandler.isInBoundsRight(positions[j]))
                        break

                    // make sure the lines don't do shitty things outside bounds
                    if (!mViewPortHandler.isInBoundsLeft(positions[j]) || !mViewPortHandler.isInBoundsY(positions[j + 1])) {
                        j += 2
                        continue
                    }

                    val entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min)

                    if (dataSet.isDrawValuesEnabled) {
                        drawValue(c,
                                dataSet.valueFormatter,
                                entry.y,
                                entry,
                                i,
                                positions[j],
                                positions[j + 1] - shapeSize,
                                dataSet.getValueTextColor(j / 2 + mXBounds.min))
                    }

                    if (entry.icon != null && dataSet.isDrawIconsEnabled) {

                        val icon = entry.icon

                        Utils.drawImage(
                                c,
                                icon,
                                (positions[j] + iconsOffset.x).toInt(),
                                (positions[j + 1] + iconsOffset.y).toInt(),
                                icon.intrinsicWidth,
                                icon.intrinsicHeight)
                    }
                    j += 2
                }

                MPPointF.recycleInstance(iconsOffset)
            }
        }
    }

    override fun drawExtras(c: Canvas) {}

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {

        val scatterData = mChart.scatterData

        for (high in indices) {

            val set = scatterData.getDataSetByIndex(high.dataSetIndex)

            if (set == null || !set.isHighlightEnabled)
                continue

            val e = set.getEntryForXValue(high.x, high.y)

            if (!isInBoundsX(e, set))
                continue

            val pix = mChart.getTransformer(set.axisDependency).getPixelForValues(e.x, e.y * mAnimator
                    .phaseY)

            high.setDraw(pix.x.toFloat(), pix.y.toFloat())

            // draw the lines
            drawHighlightLines(c, pix.x.toFloat(), pix.y.toFloat(), set)
        }
    }
}
