package cn.laplacetech.klinelib.chart

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView

import cn.laplacetech.klinelib.R
import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.util.DateUtils
import cn.laplacetech.klinelib.util.DoubleUtil
import kotlinx.android.synthetic.main.view_kline_chart_info.view.*

import java.util.Locale

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

class KLineChartInfoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ChartInfoView(context, attrs, defStyleAttr) {


    init {
        LayoutInflater.from(context).inflate(R.layout.view_kline_chart_info, this)
    }

    override fun setData(lastClose: Double, data: HisData) {
        tv_time.text = DateUtils.formatDate(data.date)
        tv_close_price.text = DoubleUtil.formatDecimal(data.close)
        tv_open_price.text = DoubleUtil.formatDecimal(data.open)
        tv_high_price.text = DoubleUtil.formatDecimal(data.high)
        tv_low_price.text = DoubleUtil.formatDecimal(data.low)
        //        mTvChangeRate.setText(String.format(Locale.getDefault(), "%.2f%%", (data.getClose()- data.getOpen()) / data.getOpen() * 100));
        if (lastClose == 0.0) {
            vg_change_rate.visibility = View.GONE
        } else {
            tv_change_rate.setText(String.format(Locale.getDefault(), "%.2f%%", (data.close!! - lastClose) / lastClose * 100))
        }
        tv_vol.text = data.vol!!.toString() + ""
        removeCallbacks(mRunnable)
        postDelayed(mRunnable, 2000)
    }

}
