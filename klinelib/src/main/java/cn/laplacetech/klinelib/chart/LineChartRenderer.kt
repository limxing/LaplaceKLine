package cn.laplacetech.klinelib.chart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable

import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider
import com.github.mikephil.charting.interfaces.datasets.IDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.renderer.BarLineScatterCandleBubbleRenderer
import com.github.mikephil.charting.renderer.LineRadarRenderer
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointD
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

import java.lang.ref.WeakReference
import java.util.HashMap
/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
open class LineChartRenderer(var mChart: LineDataProvider, animator: ChartAnimator,
                             viewPortHandler: ViewPortHandler) : LineRadarRenderer(animator, viewPortHandler) {

    /**
     * paint for the inner circle of the value indicators
     */
    protected var mCirclePaintInner: Paint

    /**
     * Bitmap object used for drawing the paths (otherwise they are too long if
     * rendered directly on the canvas)
     */
    protected var mDrawBitmap: WeakReference<Bitmap>? = null

    /**
     * on this canvas, the paths are rendered, it is initialized with the
     * pathBitmap
     */
    protected var mBitmapCanvas: Canvas? = null

    /**
     * the bitmap configuration to be used
     */
    protected var mBitmapConfig: Bitmap.Config = Bitmap.Config.ARGB_8888

    protected var cubicPath = Path()
    protected var cubicFillPath = Path()
    protected var mGenerateFilledPathBuffer = Path()
    private var mLineBuffer = FloatArray(4)
    /**
     * cache for the circle bitmaps of all datasets
     */
    private val mImageCaches = HashMap<IDataSet<*>, DataSetImageCache>()
    /**
     * buffer for drawing the circles
     */
    private val mCirclesBuffer = FloatArray(2)

    /**
     * Returns the Bitmap.Config that is used by this renderer.
     *
     * @return
     */
    /**
     * Sets the Bitmap.Config to be used by this renderer.
     * Default: Bitmap.Config.ARGB_8888
     * Use Bitmap.Config.ARGB_4444 to consume less memory.
     *
     * @param config
     */
    var bitmapConfig: Bitmap.Config
        get() = mBitmapConfig
        set(config) {
            mBitmapConfig = config
            releaseBitmap()
        }

    init {

        mCirclePaintInner = Paint(Paint.ANTI_ALIAS_FLAG)
        mCirclePaintInner.style = Paint.Style.FILL
        mCirclePaintInner.color = Color.WHITE
    }

    override fun initBuffers() {}

    override fun drawData(c: Canvas) {

        val width = mViewPortHandler.chartWidth.toInt()
        val height = mViewPortHandler.chartHeight.toInt()

        if (mDrawBitmap == null
                || mDrawBitmap!!.get()?.width != width
                || mDrawBitmap!!.get()?.height != height) {

            if (width > 0 && height > 0) {

                mDrawBitmap = WeakReference(Bitmap.createBitmap(width, height, mBitmapConfig))
                mBitmapCanvas = Canvas(mDrawBitmap!!.get())
            } else
                return
        }

        mDrawBitmap?.get()?.eraseColor(Color.TRANSPARENT)

        val lineData = mChart.lineData

        lineData.dataSets
                .filter { it.isVisible }
                .forEach { drawDataSet(c, it) }

        c.drawBitmap(mDrawBitmap!!.get(), 0f, 0f, mRenderPaint)
    }

    protected fun drawDataSet(c: Canvas, dataSet: ILineDataSet) {

        if (dataSet.entryCount < 1)
            return

        mRenderPaint.strokeWidth = dataSet.lineWidth
        mRenderPaint.pathEffect = dataSet.dashPathEffect

        when (dataSet.mode) {
            LineDataSet.Mode.LINEAR, LineDataSet.Mode.STEPPED -> drawLinear(c, dataSet)

            LineDataSet.Mode.CUBIC_BEZIER -> drawCubicBezier(dataSet)

            LineDataSet.Mode.HORIZONTAL_BEZIER -> drawHorizontalBezier(dataSet)
            else -> drawLinear(c, dataSet)
        }

        mRenderPaint.pathEffect = null
    }

    protected fun drawHorizontalBezier(dataSet: ILineDataSet) {

        val phaseY = mAnimator.phaseY

        val trans = mChart.getTransformer(dataSet.axisDependency)

        mXBounds.set(mChart, dataSet)

        cubicPath.reset()

        if (mXBounds.range >= 1) {

            var prev = dataSet.getEntryForIndex(mXBounds.min)
            var cur = prev

            // let the spline start
            cubicPath.moveTo(cur.x, cur.y * phaseY)

            for (j in mXBounds.min + 1..mXBounds.range + mXBounds.min) {

                prev = cur
                cur = dataSet.getEntryForIndex(j)

                val cpx = prev.x + (cur.x - prev.x) / 2.0f

                cubicPath.cubicTo(
                        cpx, prev.y * phaseY,
                        cpx, cur.y * phaseY,
                        cur.x, cur.y * phaseY)
            }
        }

        // if filled is enabled, close the path
        if (dataSet.isDrawFilledEnabled) {

            cubicFillPath.reset()
            cubicFillPath.addPath(cubicPath)
            // create a new path, this is bad for performance
            drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds)
        }

        mRenderPaint.color = dataSet.color

        mRenderPaint.style = Paint.Style.STROKE

        trans.pathValueToPixel(cubicPath)

        mBitmapCanvas!!.drawPath(cubicPath, mRenderPaint)

        mRenderPaint.pathEffect = null
    }

    protected fun drawCubicBezier(dataSet: ILineDataSet) {

        val phaseX = Math.max(0f, Math.min(1f, mAnimator.phaseX))
        val phaseY = mAnimator.phaseY

        val trans = mChart.getTransformer(dataSet.axisDependency)

        mXBounds.set(mChart, dataSet)

        val intensity = dataSet.cubicIntensity

        cubicPath.reset()

        if (mXBounds.range >= 1) {

            var prevDx = 0f
            var prevDy = 0f
            var curDx = 0f
            var curDy = 0f

            // Take an extra point from the left, and an extra from the right.
            // That's because we need 4 points for a cubic bezier (cubic=4), otherwise we get lines moving and doing weird stuff on the edges of the chart.
            // So in the starting `prev` and `cur`, go -2, -1
            // And in the `lastIndex`, add +1

            val firstIndex = mXBounds.min + 1
            val lastIndex = mXBounds.min + mXBounds.range

            var prevPrev: Entry
            var prev = dataSet.getEntryForIndex(Math.max(firstIndex - 2, 0))
            var cur: Entry? = dataSet.getEntryForIndex(Math.max(firstIndex - 1, 0))
            var next = cur
            var nextIndex = -1

            if (cur == null) return

            // let the spline start
            cubicPath.moveTo(cur.x, cur.y * phaseY)

            for (j in mXBounds.min + 1..mXBounds.range + mXBounds.min) {

                prevPrev = prev
                prev = cur
                cur = if (nextIndex == j) next else dataSet.getEntryForIndex(j)

                nextIndex = if (j + 1 < dataSet.entryCount) j + 1 else j
                next = dataSet.getEntryForIndex(nextIndex)

                prevDx = (cur!!.x - prevPrev.x) * intensity
                prevDy = (cur.y - prevPrev.y) * intensity
                curDx = (next!!.x - prev.x) * intensity
                curDy = (next.y - prev.y) * intensity

                cubicPath.cubicTo(prev.x + prevDx, (prev.y + prevDy) * phaseY,
                        cur.x - curDx,
                        (cur.y - curDy) * phaseY, cur.x, cur.y * phaseY)
            }
        }

        // if filled is enabled, close the path
        if (dataSet.isDrawFilledEnabled) {

            cubicFillPath.reset()
            cubicFillPath.addPath(cubicPath)

            drawCubicFill(mBitmapCanvas, dataSet, cubicFillPath, trans, mXBounds)
        }

        mRenderPaint.color = dataSet.color

        mRenderPaint.style = Paint.Style.STROKE

        trans.pathValueToPixel(cubicPath)

        mBitmapCanvas!!.drawPath(cubicPath, mRenderPaint)

        mRenderPaint.pathEffect = null
    }

    protected fun drawCubicFill(c: Canvas?, dataSet: ILineDataSet, spline: Path, trans: Transformer, bounds: BarLineScatterCandleBubbleRenderer.XBounds) {

        val fillMin = dataSet.fillFormatter
                .getFillLinePosition(dataSet, mChart)

        spline.lineTo(dataSet.getEntryForIndex(bounds.min + bounds.range).x, fillMin)
        spline.lineTo(dataSet.getEntryForIndex(bounds.min).x, fillMin)
        spline.close()

        trans.pathValueToPixel(spline)

        val drawable = dataSet.fillDrawable
        if (drawable != null) {

            drawFilledPath(c, spline, drawable)
        } else {

            drawFilledPath(c, spline, dataSet.fillColor, dataSet.fillAlpha)
        }
    }

    /**
     * Draws a normal line.
     */
    protected fun drawLinear(c: Canvas, dataSet: ILineDataSet) {

        val entryCount = dataSet.entryCount

        val isDrawSteppedEnabled = dataSet.isDrawSteppedEnabled
        val pointsPerEntryPair = if (isDrawSteppedEnabled) 4 else 2

        val trans = mChart.getTransformer(dataSet.axisDependency)

        val phaseY = mAnimator.phaseY

        mRenderPaint.style = Paint.Style.STROKE

        var canvas: Canvas? = null

        // if the data-set is dashed, draw on bitmap-canvas
        if (dataSet.isDashedLineEnabled) {
            canvas = mBitmapCanvas
        } else {
            canvas = c
        }

        mXBounds.set(mChart, dataSet)

        // if drawing filled is enabled
        if (dataSet.isDrawFilledEnabled && entryCount > 0) {
            drawLinearFill(c, dataSet, trans, mXBounds)
        }

        // more than 1 color
        if (dataSet.colors.size > 1) {

            if (mLineBuffer.size <= pointsPerEntryPair * 2)
                mLineBuffer = FloatArray(pointsPerEntryPair * 4)

            for (j in mXBounds.min..mXBounds.range + mXBounds.min) {

                var e: Entry? = dataSet.getEntryForIndex(j) ?: continue

                mLineBuffer[0] = e!!.x
                mLineBuffer[1] = e.y * phaseY

                if (j < mXBounds.max) {

                    e = dataSet.getEntryForIndex(j + 1)

                    if (e == null) break

                    if (isDrawSteppedEnabled) {
                        mLineBuffer[2] = e.x
                        mLineBuffer[3] = mLineBuffer[1]
                        mLineBuffer[4] = mLineBuffer[2]
                        mLineBuffer[5] = mLineBuffer[3]
                        mLineBuffer[6] = e.x
                        mLineBuffer[7] = e.y * phaseY
                    } else {
                        mLineBuffer[2] = e.x
                        mLineBuffer[3] = e.y * phaseY
                    }

                } else {
                    mLineBuffer[2] = mLineBuffer[0]
                    mLineBuffer[3] = mLineBuffer[1]
                }

                trans.pointValuesToPixel(mLineBuffer)

                if (!mViewPortHandler.isInBoundsRight(mLineBuffer[0]))
                    break

                // make sure the lines don't do shitty things outside
                // bounds
                if (!mViewPortHandler.isInBoundsLeft(mLineBuffer[2]) || !mViewPortHandler.isInBoundsTop(mLineBuffer[1]) && !mViewPortHandler
                                .isInBoundsBottom(mLineBuffer[3]))
                    continue

                // get the color that is set for this line-segment
                mRenderPaint.color = dataSet.getColor(j)

                canvas!!.drawLines(mLineBuffer, 0, pointsPerEntryPair * 2, mRenderPaint)
            }

        } else { // only one color per dataset

            if (mLineBuffer.size < Math.max(entryCount * pointsPerEntryPair, pointsPerEntryPair) * 2)
                mLineBuffer = FloatArray(Math.max(entryCount * pointsPerEntryPair, pointsPerEntryPair) * 4)

            var e1: Entry?
            var e2: Entry?

            e1 = dataSet.getEntryForIndex(mXBounds.min)

            if (e1 != null) {

                var j = 0
                for (x in mXBounds.min..mXBounds.range + mXBounds.min) {

                    e1 = dataSet.getEntryForIndex(if (x == 0) 0 else x - 1)
                    e2 = dataSet.getEntryForIndex(x)

                    if (e1 == null || e2 == null) continue

                    mLineBuffer[j++] = e1.x
                    mLineBuffer[j++] = e1.y * phaseY

                    if (isDrawSteppedEnabled) {
                        mLineBuffer[j++] = e2.x
                        mLineBuffer[j++] = e1.y * phaseY
                        mLineBuffer[j++] = e2.x
                        mLineBuffer[j++] = e1.y * phaseY
                    }

                    mLineBuffer[j++] = e2.x
                    mLineBuffer[j++] = e2.y * phaseY
                }

                if (j > 0) {
                    trans.pointValuesToPixel(mLineBuffer)

                    val size = Math.max((mXBounds.range + 1) * pointsPerEntryPair, pointsPerEntryPair) * 2

                    mRenderPaint.color = dataSet.color

                    canvas!!.drawLines(mLineBuffer, 0, size, mRenderPaint)
                }
            }
        }

        mRenderPaint.pathEffect = null
    }

    /**
     * Draws a filled linear path on the canvas.
     *
     * @param c
     * @param dataSet
     * @param trans
     * @param bounds
     */
    protected fun drawLinearFill(c: Canvas, dataSet: ILineDataSet, trans: Transformer, bounds: BarLineScatterCandleBubbleRenderer.XBounds) {

        val filled = mGenerateFilledPathBuffer

        val startingIndex = bounds.min
        val endingIndex = bounds.range + bounds.min
        val indexInterval = 128

        var currentStartIndex = 0
        var currentEndIndex = indexInterval
        var iterations = 0

        // Doing this iteratively in order to avoid OutOfMemory errors that can happen on large bounds sets.
        do {
            currentStartIndex = startingIndex + iterations * indexInterval
            currentEndIndex = currentStartIndex + indexInterval
            currentEndIndex = if (currentEndIndex > endingIndex) endingIndex else currentEndIndex

            if (currentStartIndex <= currentEndIndex) {
                generateFilledPath(dataSet, currentStartIndex, currentEndIndex, filled)

                trans.pathValueToPixel(filled)

                val drawable = dataSet.fillDrawable
                if (drawable != null) {

                    drawFilledPath(c, filled, drawable)
                } else {

                    drawFilledPath(c, filled, dataSet.fillColor, dataSet.fillAlpha)
                }
            }

            iterations++

        } while (currentStartIndex <= currentEndIndex)

    }

    /**
     * Generates a path that is used for filled drawing.
     *
     * @param dataSet    The dataset from which to read the entries.
     * @param startIndex The index from which to start reading the dataset
     * @param endIndex   The index from which to stop reading the dataset
     * @param outputPath The path object that will be assigned the chart data.
     * @return
     */
    private fun generateFilledPath(dataSet: ILineDataSet, startIndex: Int, endIndex: Int, outputPath: Path) {

        val fillMin = dataSet.fillFormatter.getFillLinePosition(dataSet, mChart)
        val phaseY = mAnimator.phaseY
        val isDrawSteppedEnabled = dataSet.mode == LineDataSet.Mode.STEPPED

        outputPath.reset()

        val entry = dataSet.getEntryForIndex(startIndex)

        outputPath.moveTo(entry.x, fillMin)
        outputPath.lineTo(entry.x, entry.y * phaseY)

        // create a new path
        var currentEntry: Entry? = null
        var previousEntry: Entry? = null
        for (x in startIndex + 1..endIndex) {

            currentEntry = dataSet.getEntryForIndex(x)

            if (isDrawSteppedEnabled && previousEntry != null) {
                outputPath.lineTo(currentEntry!!.x, previousEntry.y * phaseY)
            }

            outputPath.lineTo(currentEntry!!.x, currentEntry.y * phaseY)

            previousEntry = currentEntry
        }

        // close up
        if (currentEntry != null) {
            outputPath.lineTo(currentEntry.x, fillMin)
        }

        outputPath.close()
    }

    override fun drawValues(c: Canvas) {

        if (isDrawingValuesAllowed(mChart)) {

            val dataSets = mChart.lineData.dataSets

            for (i in dataSets.indices) {

                val dataSet = dataSets[i]

                if (!shouldDrawValues(dataSet))
                    continue

                // apply the text-styling defined by the DataSet
                applyValueTextStyle(dataSet)

                val trans = mChart.getTransformer(dataSet.axisDependency)

                // make sure the values do not interfear with the circles
                var valOffset = (dataSet.circleRadius * 1.75f).toInt()

                if (!dataSet.isDrawCirclesEnabled)
                    valOffset = valOffset / 2

                mXBounds.set(mChart, dataSet)

                val positions = trans.generateTransformedValuesLine(dataSet, mAnimator.phaseX, mAnimator
                        .phaseY, mXBounds.min, mXBounds.max)

                val iconsOffset = MPPointF.getInstance(dataSet.iconsOffset)
                iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x)
                iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y)

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

                    val entry = dataSet.getEntryForIndex(j / 2 + mXBounds.min)

                    if (dataSet.isDrawValuesEnabled) {
                        drawValue(c, dataSet.valueFormatter, entry.y, entry, i, x,
                                y - valOffset, dataSet.getValueTextColor(j / 2))
                    }

                    if (entry.icon != null && dataSet.isDrawIconsEnabled) {

                        val icon = entry.icon

                        Utils.drawImage(
                                c,
                                icon,
                                (x + iconsOffset.x).toInt(),
                                (y + iconsOffset.y).toInt(),
                                icon.intrinsicWidth,
                                icon.intrinsicHeight)
                    }
                    j += 2
                }

                MPPointF.recycleInstance(iconsOffset)
            }
        }
    }

    override fun drawExtras(c: Canvas) {
        drawCircles(c)
    }

    protected fun drawCircles(c: Canvas) {

        mRenderPaint.style = Paint.Style.FILL

        val phaseY = mAnimator.phaseY

        mCirclesBuffer[0] = 0f
        mCirclesBuffer[1] = 0f

        val dataSets = mChart.lineData.dataSets

        for (i in dataSets.indices) {

            val dataSet = dataSets[i]

            if (!dataSet.isVisible || !dataSet.isDrawCirclesEnabled ||
                    dataSet.entryCount == 0)
                continue

            mCirclePaintInner.color = dataSet.circleHoleColor

            val trans = mChart.getTransformer(dataSet.axisDependency)

            mXBounds.set(mChart, dataSet)

            val circleRadius = dataSet.circleRadius
            val circleHoleRadius = dataSet.circleHoleRadius
            val drawCircleHole = dataSet.isDrawCircleHoleEnabled &&
                    circleHoleRadius < circleRadius &&
                    circleHoleRadius > 0f
            val drawTransparentCircleHole = drawCircleHole && dataSet.circleHoleColor == ColorTemplate.COLOR_NONE

            var imageCache: DataSetImageCache? = null

            if (mImageCaches.containsKey(dataSet)) {
                imageCache = mImageCaches[dataSet]
            } else {
                imageCache = DataSetImageCache()
                mImageCaches[dataSet] = imageCache
            }

            val changeRequired = imageCache?.init(dataSet)

            // only fill the cache with new bitmaps if a change is required
            if (changeRequired == true) {
                imageCache?.fill(dataSet, drawCircleHole, drawTransparentCircleHole)
            }

            val boundsRangeCount = mXBounds.range + mXBounds.min

            for (j in mXBounds.min..boundsRangeCount) {

                val e = dataSet.getEntryForIndex(j) ?: break

                mCirclesBuffer[0] = e.x
                mCirclesBuffer[1] = e.y * phaseY

                trans.pointValuesToPixel(mCirclesBuffer)

                if (!mViewPortHandler.isInBoundsRight(mCirclesBuffer[0]))
                    break

                if (!mViewPortHandler.isInBoundsLeft(mCirclesBuffer[0]) || !mViewPortHandler.isInBoundsY(mCirclesBuffer[1]))
                    continue

                val circleBitmap = imageCache?.getBitmap(j)

                if (circleBitmap != null) {
                    c.drawBitmap(circleBitmap, mCirclesBuffer[0] - circleRadius, mCirclesBuffer[1] - circleRadius, null)
                }
            }
        }
    }

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {

        val lineData = mChart.lineData

        for (high in indices) {

            val set = lineData.getDataSetByIndex(high.dataSetIndex)

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

            // draw circle
            //            drawCircle(c, high, (float) pix.x);
        }
    }

    fun drawCircle(c: Canvas, high: Highlight, x: Float) {
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

            if (high.x < dataSet.entryCount - 1) {
                val e = dataSet.getEntryForIndex(high.x.toInt()) ?: return

                mCirclesBuffer[1] = e.y * phaseY

                trans.pointValuesToPixel(mCirclesBuffer)

                mCirclesBuffer[0] = x
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

    /**
     * Releases the drawing bitmap. This should be called when [LineChart.onDetachedFromWindow].
     */
    fun releaseBitmap() {
        if (mBitmapCanvas != null) {
            mBitmapCanvas?.setBitmap(null)
            mBitmapCanvas = null
        }
        if (mDrawBitmap != null) {
            mDrawBitmap?.get()?.recycle()
            mDrawBitmap?.clear()
            mDrawBitmap = null
        }
    }

    private inner class DataSetImageCache {

        private val mCirclePathBuffer = Path()

        private var circleBitmaps: Array<Bitmap?>? = null

        /**
         * Sets up the cache, returns true if a change of cache was required.
         *
         * @param set
         * @return
         */
        fun init(set: ILineDataSet): Boolean {

            val size = set.circleColorCount
            var changeRequired = false

            if (circleBitmaps == null) {
                circleBitmaps = arrayOfNulls(size)
                changeRequired = true
            } else if (circleBitmaps!!.size != size) {
                circleBitmaps = arrayOfNulls(size)
                changeRequired = true
            }

            return changeRequired
        }

        /**
         * Fills the cache with bitmaps for the given dataset.
         *
         * @param set
         * @param drawCircleHole
         * @param drawTransparentCircleHole
         */
        fun fill(set: ILineDataSet, drawCircleHole: Boolean, drawTransparentCircleHole: Boolean) {

            val colorCount = set.circleColorCount
            val circleRadius = set.circleRadius
            val circleHoleRadius = set.circleHoleRadius

            for (i in 0 until colorCount) {

                val conf = Bitmap.Config.ARGB_4444
                val circleBitmap = Bitmap.createBitmap((circleRadius * 2.1).toInt(), (circleRadius * 2.1).toInt(), conf)

                val canvas = Canvas(circleBitmap)
                circleBitmaps?.set(i, circleBitmap)
                mRenderPaint.color = set.getCircleColor(i)

                if (drawTransparentCircleHole) {
                    // Begin path for circle with hole
                    mCirclePathBuffer.reset()

                    mCirclePathBuffer.addCircle(
                            circleRadius,
                            circleRadius,
                            circleRadius,
                            Path.Direction.CW)

                    // Cut hole in path
                    mCirclePathBuffer.addCircle(
                            circleRadius,
                            circleRadius,
                            circleHoleRadius,
                            Path.Direction.CCW)

                    // Fill in-between
                    canvas.drawPath(mCirclePathBuffer, mRenderPaint)
                } else {

                    canvas.drawCircle(
                            circleRadius,
                            circleRadius,
                            circleRadius,
                            mRenderPaint)

                    if (drawCircleHole) {
                        canvas.drawCircle(
                                circleRadius,
                                circleRadius,
                                circleHoleRadius,
                                mCirclePaintInner)
                    }
                }
            }
        }

        /**
         * Returns the cached Bitmap at the given index.
         *
         * @param index
         * @return
         */
        fun getBitmap(index: Int): Bitmap? {
            return circleBitmaps!![index % circleBitmaps!!.size]
        }
    }
}
