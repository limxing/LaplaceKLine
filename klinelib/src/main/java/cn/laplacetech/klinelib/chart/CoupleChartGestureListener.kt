package cn.laplacetech.klinelib.chart


import android.graphics.Matrix
import android.util.Log
import android.view.MotionEvent

import com.github.mikephil.charting.charts.BarLineChartBase
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener

/**
 * http://stackoverflow.com/questions/28521004/mpandroidchart-have-one-graph-mirror-the-zoom-swipes-on-a-sister-graph
 */
class CoupleChartGestureListener(private val srcChart: BarLineChartBase<*>, vararg dstCharts: Chart<*>) : OnChartGestureListener {

    private var listener: OnAxisChangeListener? = null

    private var mOnLoadMoreListener: OnLoadMoreListener? = null

    private var isLoadMore = false

    fun setOnLoadMoreListener(onLoadMoreListener: OnLoadMoreListener) {
        mOnLoadMoreListener = onLoadMoreListener
    }

    constructor(listener: OnAxisChangeListener, srcChart: BarLineChartBase<*>, vararg dstCharts: Chart<*>) : this(srcChart, *dstCharts) {
        this.listener = listener
    }

    private var dstCharts: Array<out Chart<*>> = dstCharts

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        syncCharts()
    }

    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        syncCharts()
    }

    override fun onChartLongPressed(me: MotionEvent) {
        syncCharts()
    }

    override fun onChartDoubleTapped(me: MotionEvent) {
        syncCharts()
    }

    override fun onChartSingleTapped(me: MotionEvent) {
        syncCharts()
    }

    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) {
        listener?.onAxisChange(srcChart)
        performLoadMore()
        syncCharts()
    }

    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) {
        //        Log.d(TAG, "onChartScale " + scaleX + "/" + scaleY + " X=" + me.getX() + "Y=" + me.getY());
        listener?.onAxisChange(srcChart)
        performLoadMore()
        syncCharts()
    }

    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) {
        //        Log.d(TAG, "onChartTranslate " + dX + "/" + dY + " X=" + me.getX() + "Y=" + me.getY());
        Log.d(TAG, srcChart.lowestVisibleX.toString() + "")
        //        Log.d(TAG, "getHighestVisibleX  " +srcChart.getHighestVisibleX());
        listener?.onAxisChange(srcChart)
        performLoadMore()
        syncCharts()
    }

    private fun performLoadMore() {
        // 加载更多
        if (mOnLoadMoreListener != null && !isLoadMore) {
            if (srcChart.lowestVisibleX <= 0) {
                isLoadMore = true
                mOnLoadMoreListener!!.onLoadMore()
            }
        }
    }

    private fun syncCharts() {
        val srcMatrix: Matrix
        val srcVals = FloatArray(9)
        var dstMatrix: Matrix
        val dstVals = FloatArray(9)
        // get src chart translation matrix:
        srcMatrix = srcChart.viewPortHandler.matrixTouch
        srcMatrix.getValues(srcVals)
        // apply X axis scaling and position to dst charts:
        for (dstChart in dstCharts) {
            dstMatrix = dstChart.viewPortHandler.matrixTouch
            dstMatrix.getValues(dstVals)

            dstVals[Matrix.MSCALE_X] = srcVals[Matrix.MSCALE_X]
            dstVals[Matrix.MSKEW_X] = srcVals[Matrix.MSKEW_X]
            dstVals[Matrix.MTRANS_X] = srcVals[Matrix.MTRANS_X]
            dstVals[Matrix.MSKEW_Y] = srcVals[Matrix.MSKEW_Y]
            dstVals[Matrix.MSCALE_Y] = srcVals[Matrix.MSCALE_Y]
            dstVals[Matrix.MTRANS_Y] = srcVals[Matrix.MTRANS_Y]
            dstVals[Matrix.MPERSP_0] = srcVals[Matrix.MPERSP_0]
            dstVals[Matrix.MPERSP_1] = srcVals[Matrix.MPERSP_1]
            dstVals[Matrix.MPERSP_2] = srcVals[Matrix.MPERSP_2]

            dstMatrix.setValues(dstVals)
            dstChart.viewPortHandler.refresh(dstMatrix, dstChart, true)
        }
    }

    fun loadMoreComplete() {
        isLoadMore = false
    }

    interface OnAxisChangeListener {
        fun onAxisChange(chart: BarLineChartBase<*>)
    }

    companion object {

        private val TAG = CoupleChartGestureListener::class.java.simpleName
    }

}