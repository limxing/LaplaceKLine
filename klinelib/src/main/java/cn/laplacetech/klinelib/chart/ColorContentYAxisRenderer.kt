package cn.laplacetech.klinelib.chart

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.renderer.YAxisRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

import com.github.mikephil.charting.utils.Utils.convertDpToPixel


/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 * 为每个label设置颜色，把两端的label绘制在内容区域内
 */

class ColorContentYAxisRenderer(viewPortHandler: ViewPortHandler, yAxis: YAxis, trans: Transformer) : YAxisRenderer(viewPortHandler, yAxis, trans) {
    private var mLabelColorArray: IntArray? = null
    private var mLabelInContent = false
    private var mUseDefaultLabelXOffset = true
    private var mUseDefaultLimitLineLabelXOffset = true

    /**
     * 给每个label单独设置颜色
     */
    fun setLabelColor(labelColorArray: IntArray) {
        mLabelColorArray = labelColorArray
    }

    fun setLabelInContent(flag: Boolean) {
        mLabelInContent = flag
    }

    fun setUseDefaultLabelXOffset(flag: Boolean) {
        mUseDefaultLabelXOffset = flag
    }

    fun setUseDefaultLimitLineLabelXOffset(flag: Boolean) {
        mUseDefaultLimitLineLabelXOffset = flag
    }

    override fun renderAxisLabels(c: Canvas) {
        if (!mYAxis.isEnabled || !mYAxis.isDrawLabelsEnabled)
            return

        val positions = transformedPositions

        mAxisLabelPaint.typeface = mYAxis.typeface
        mAxisLabelPaint.textSize = mYAxis.textSize
        mAxisLabelPaint.color = mYAxis.textColor

        val xoffset = mYAxis.xOffset
        val yoffset = Utils.calcTextHeight(mAxisLabelPaint, "A") / 2.5f + mYAxis.yOffset

        val dependency = mYAxis.axisDependency
        val labelPosition = mYAxis.labelPosition

        var xPos: Float

        if (dependency == YAxis.AxisDependency.LEFT) {

            if (labelPosition == YAxis.YAxisLabelPosition.OUTSIDE_CHART) {
                mAxisLabelPaint.textAlign = Paint.Align.RIGHT
                xPos = mViewPortHandler.offsetLeft() - xoffset
            } else {
                mAxisLabelPaint.textAlign = Paint.Align.LEFT
                xPos = mViewPortHandler.offsetLeft() + xoffset
            }

        } else {

            if (labelPosition == YAxis.YAxisLabelPosition.OUTSIDE_CHART) {
                mAxisLabelPaint.textAlign = Paint.Align.LEFT
                xPos = mViewPortHandler.contentRight() + xoffset
            } else {
                mAxisLabelPaint.textAlign = Paint.Align.RIGHT
                xPos = mViewPortHandler.contentRight() - xoffset
            }
        }

        drawYLabels(c, xPos, positions, yoffset)
    }

    override fun drawYLabels(c: Canvas, fixedPositionR: Float, positions: FloatArray, offset: Float) {
        var fixedPosition = fixedPositionR

        val from = if (mYAxis.isDrawBottomYLabelEntryEnabled) 0 else 1
        val to = if (mYAxis.isDrawTopYLabelEntryEnabled)
            mYAxis.mEntryCount
        else
            mYAxis.mEntryCount - 1

        val originalColor = mAxisLabelPaint.color

        val textHeight = Utils.calcTextHeight(mAxisLabelPaint, "A").toFloat()
        val yoffset = textHeight / 2.5f + mYAxis.yOffset
        val space = Utils.convertDpToPixel(1f)
        if (!mUseDefaultLabelXOffset) {
            if (mYAxis.axisDependency == YAxis.AxisDependency.LEFT) {
                fixedPosition -= mYAxis.xOffset
            } else {
                fixedPosition += mYAxis.xOffset
            }
        }

        // draw
        for (i in from until to) {

            if (mLabelColorArray != null && i >= 0 && i < mLabelColorArray!!.size) {
                val labelColor = mLabelColorArray!![i]
                mAxisLabelPaint.color = labelColor
            } else {
                mAxisLabelPaint.color = originalColor
            }
            val text = mYAxis.getFormattedLabel(i)

            var y = positions[i * 2 + 1] + offset

            if (mLabelInContent) {
                if (i == from) {
                    y = y - offset - space - 1f
                } else if (i == to - 1) {
                    y = y - yoffset + textHeight + space + 1f
                }
            }

            c.drawText(text, fixedPosition, y, mAxisLabelPaint)
        }
        mAxisLabelPaint.color = originalColor
    }

    override fun renderLimitLines(c: Canvas) {

        val limitLines = mYAxis.limitLines

        if (limitLines == null || limitLines.size <= 0)
            return

        val pts = mRenderLimitLinesBuffer
        pts[0] = 0f
        pts[1] = 0f
        val limitLinePath = mRenderLimitLines
        limitLinePath.reset()

        for (i in limitLines.indices) {

            val l = limitLines[i]

            if (!l.isEnabled)
                continue

            val clipRestoreCount = c.save()
            mLimitLineClippingRect.set(mViewPortHandler.contentRect)
            mLimitLineClippingRect.inset(0f, -l.lineWidth)
            c.clipRect(mLimitLineClippingRect)

            mLimitLinePaint.style = Paint.Style.STROKE
            mLimitLinePaint.color = l.lineColor
            mLimitLinePaint.strokeWidth = l.lineWidth
            mLimitLinePaint.pathEffect = l.dashPathEffect

            pts[1] = l.limit

            mTrans.pointValuesToPixel(pts)

            limitLinePath.moveTo(mViewPortHandler.contentLeft(), pts[1])
            limitLinePath.lineTo(mViewPortHandler.contentRight(), pts[1])

            c.drawPath(limitLinePath, mLimitLinePaint)
            limitLinePath.reset()
            // c.drawLines(pts, mLimitLinePaint);

            val label = l.label

            // if drawing the limit-value label is enabled
            if (label != null && label != "") {

                mLimitLinePaint.style = l.textStyle
                mLimitLinePaint.pathEffect = null
                mLimitLinePaint.color = l.textColor
                mLimitLinePaint.typeface = l.typeface
                mLimitLinePaint.strokeWidth = 0.5f
                mLimitLinePaint.textSize = l.textSize

                val labelLineHeight = Utils.calcTextHeight(mLimitLinePaint, label).toFloat()
                var xOffset = convertDpToPixel(4f) + l.xOffset
                val yOffset = l.lineWidth + labelLineHeight + l.yOffset

                val position = l.labelPosition

                if (!mUseDefaultLimitLineLabelXOffset) {
                    xOffset = convertDpToPixel(1f)
                }

                if (position == LimitLine.LimitLabelPosition.RIGHT_TOP) {

                    mLimitLinePaint.textAlign = Paint.Align.RIGHT
                    c.drawText(label,
                            mViewPortHandler.contentRight() - xOffset,
                            pts[1] - yOffset + labelLineHeight, mLimitLinePaint)

                } else if (position == LimitLine.LimitLabelPosition.RIGHT_BOTTOM) {

                    mLimitLinePaint.textAlign = Paint.Align.RIGHT
                    c.drawText(label,
                            mViewPortHandler.contentRight() - xOffset,
                            pts[1] + yOffset, mLimitLinePaint)

                } else if (position == LimitLine.LimitLabelPosition.LEFT_TOP) {

                    mLimitLinePaint.textAlign = Paint.Align.LEFT
                    c.drawText(label,
                            mViewPortHandler.contentLeft() + xOffset,
                            pts[1] - yOffset + labelLineHeight, mLimitLinePaint)

                } else {

                    mLimitLinePaint.textAlign = Paint.Align.LEFT
                    c.drawText(label,
                            mViewPortHandler.offsetLeft() + xOffset,
                            pts[1] + yOffset, mLimitLinePaint)
                }
            }

            c.restoreToCount(clipRestoreCount)
        }
    }
}
