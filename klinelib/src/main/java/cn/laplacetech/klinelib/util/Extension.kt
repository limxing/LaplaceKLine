package cn.laplacetech.klinelib.util

import android.content.Context
import android.os.Build
import android.support.v4.content.ContextCompat
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