package cn.laplacetech.klinelib.model

import java.util.ArrayList
/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class RSV(OHLCData: List<HisData>?, private val n: Int) {
    internal val rsv: ArrayList<Double> = ArrayList()
    internal var high = 0.0
    internal var low = 0.0
    internal var close = 0.0

    init {
        val r = ArrayList<Double>()
        var rs = 0.0

        if (OHLCData != null && OHLCData.isNotEmpty()) {

            for (i in OHLCData.indices.reversed()) {
                val oHLCEntity = OHLCData[i]
                high = oHLCEntity.high ?: 0.0
                low = oHLCEntity.low ?: 0.0
                close = oHLCEntity.close ?: 0.0
                if (OHLCData.size - i < n) {
                    for (j in 0 until OHLCData.size - i) {
                        val oHLCEntity1 = OHLCData[i + j]
                        high = if (high > oHLCEntity1.high ?: 0.0) high else oHLCEntity1.high  ?: 0.0
                        low = if (low < oHLCEntity1.low  ?: 0.0) low else oHLCEntity1.low  ?: 0.0
                    }
                } else {
                    for (j in 0 until n) {
                        val oHLCEntity1 = OHLCData[i + j]
                        high = if (high > oHLCEntity1.high ?: 0.0) high else oHLCEntity1.high ?: 0.0
                        low = if (low < oHLCEntity1.low ?: 0.0) low else oHLCEntity1.low ?: 0.0
                    }
                }
                if (high != low) {
                    rs = (close - low) / (high - low) * 100
                    r.add(rs)
                } else {
                    r.add(0.00)
                }
            }
            r.indices.reversed().mapTo(rsv) { r[it] }
        }
    }

    fun getRSV(): List<Double> {
        return rsv
    }
}

