package cn.laplacetech.klinelib.chart

import android.graphics.Canvas
import android.graphics.Paint

import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

class CustomLineChartRenderer(chart: LineDataProvider, animator: ChartAnimator, viewPortHandler: ViewPortHandler) : LineChartRenderer(chart, animator, viewPortHandler) {


    private val mCirclesBuffer = FloatArray(2)

    /* @Override
    protected void drawHighlightLines(Canvas c, float x, float y, ILineScatterCandleRadarDataSet set) {
        // set color and stroke-width
        mHighlightPaint.setColor(set.getHighLightColor());
        mHighlightPaint.setStrokeWidth(set.getHighlightLineWidth());

        // draw highlighted lines (if enabled)
        mHighlightPaint.setPathEffect(set.getDashPathEffectHighlight());

        // draw vertical highlight lines
        if (set.isVerticalHighlightIndicatorEnabled()) {

            // create vertical path
            mHighlightLinePath.reset();
            mHighlightLinePath.moveTo(x, mViewPortHandler.contentTop());
            mHighlightLinePath.lineTo(x, mViewPortHandler.contentBottom());

            c.drawPath(mHighlightLinePath, mHighlightPaint);
        }

        // draw horizontal highlight lines
        if (set.isHorizontalHighlightIndicatorEnabled()) {

            // create horizontal path
            mHighlightLinePath.reset();
//            mHighlightLinePath.moveTo(mViewPortHandler.contentLeft(), y);
            mHighlightLinePath.moveTo(x, y);
            mHighlightLinePath.lineTo(mViewPortHandler.contentRight(), y);

            c.drawPath(mHighlightLinePath, mHighlightPaint);
        }
    }*/

    override fun drawExtras(c: Canvas) {
        super.drawExtras(c)
        drawLastPointCircle(c)
    }


    protected fun drawLastPointCircle(c: Canvas) {

        mRenderPaint.style = Paint.Style.FILL
        val phaseY = mAnimator.phaseY
        mCirclesBuffer[0] = 0f
        mCirclesBuffer[1] = 0f

        val dataSets = mChart.lineData.dataSets

        for (i in dataSets.indices) {

            val dataSet = dataSets[i]

            if (!dataSet.isVisible /*|| !dataSet.isDrawCirclesEnabled()*/ || dataSet.entryCount == 0)
                continue

            mRenderPaint.color = dataSet.getCircleColor(0)
            mCirclePaintInner.color = dataSet.circleHoleColor

            val trans = mChart.getTransformer(dataSet.axisDependency)

            mXBounds.set(mChart, dataSet)

            val circleRadius = dataSet.circleRadius * 2.0f
            val circleHoleRadius = dataSet.circleHoleRadius * 2.0f
            val drawCircleHole = dataSet.isDrawCircleHoleEnabled &&
                    circleHoleRadius < circleRadius &&
                    circleHoleRadius > 0f

            val e = dataSet.getEntryForIndex(dataSet.entryCount - 1) ?: return

            mCirclesBuffer[0] = e.x
            mCirclesBuffer[1] = e.y * phaseY

            trans.pointValuesToPixel(mCirclesBuffer)

            if (!mViewPortHandler.isInBoundsRight(mCirclesBuffer[0]))
                return

            if (!mViewPortHandler.isInBoundsLeft(mCirclesBuffer[0]) || !mViewPortHandler.isInBoundsY(mCirclesBuffer[1]))
                return

            c.drawCircle(
                    mCirclesBuffer[0],
                    mCirclesBuffer[1],
                    circleRadius,
                    mRenderPaint)

            if (drawCircleHole) {
                c.drawCircle(
                        mCirclesBuffer[0],
                        mCirclesBuffer[1],
                        circleHoleRadius,
                        mCirclePaintInner)
            }
        }
    }
}
