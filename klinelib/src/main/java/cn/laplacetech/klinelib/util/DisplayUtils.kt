package cn.laplacetech.klinelib.util

import android.content.Context
import android.view.WindowManager

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
object DisplayUtils {

    fun getWidthHeight(context: Context): IntArray {
        val wm = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val width = wm.defaultDisplay.width
        val height = wm.defaultDisplay.height

        return intArrayOf(width, height)
    }

    fun dip2px(context: Context, dipValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dipValue * scale + 0.5f).toInt()
    }

    fun px2dip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }


}
