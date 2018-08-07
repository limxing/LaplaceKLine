package cn.laplacetech.klinelib.model

import java.util.ArrayList

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class KDJ(OHLCData: List<HisData>?) {
    val k: ArrayList<Double>
    val d: ArrayList<Double>
    val j: ArrayList<Double>
    private var rsv: ArrayList<Double>? = null

    private val mRSV: RSV

    init {
        k = ArrayList()
        d = ArrayList()
        j = ArrayList()

        val ks = ArrayList<Double>()
        val ds = ArrayList<Double>()
        val js = ArrayList<Double>()

        mRSV = RSV(OHLCData, 9)
        var k = 50.0
        var d = 50.0
        var j = 0.0
        var rSV = 0.0

        if (OHLCData != null && OHLCData.isNotEmpty()) {
            rsv = mRSV.rsv
            for (i in OHLCData.indices.reversed()) {
                rSV = rsv!![i]
                k = (k * 2 + rSV) / 3
                d = (d * 2 + k) / 3
                j = 3 * k - 2 * d
                ks.add(k)
                ds.add(d)
                js.add(j)
            }
            for (i in ks.indices.reversed()) {
                this.k.add(ks[i])
                this.d.add(ds[i])
                this.j.add(js[i])
            }
        }
    }
}

