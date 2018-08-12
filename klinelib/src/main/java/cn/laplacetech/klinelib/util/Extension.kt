package cn.laplacetech.klinelib.util

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
import android.view.View
import cn.laplacetech.klinelib.chart.BaseView

/**
 * Created by laplace on 2018/8/7.
 */
fun BaseView.getColor(id: Int): Int {
    return if (Build.VERSION.SDK_INT >= 23) {
        ContextCompat.getColor(context, id)
    } else {
        resources.getColor(id)
    }


}

fun View.isVisible(): Boolean {
    return visibility == View.VISIBLE
}

fun View.visibleOrGone(visible: Boolean) {
    visibility = if (visible) {
        View.VISIBLE
    } else {
        View.GONE
    }
}
