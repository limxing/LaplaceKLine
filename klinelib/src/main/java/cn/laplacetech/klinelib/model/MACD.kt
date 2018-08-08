package cn.laplacetech.klinelib.model

import java.util.ArrayList

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
class MACD
/**
 * 得到MACD数据
 *
 * @param kLineBeen
 */
(kLineBeen: List<HisData>?) {

    private val DEAs: MutableList<Double>
    private val DIFs: MutableList<Double>
    private val MACDs: MutableList<Double>

    val dea: List<Double>
        get() = DEAs

    val dif: List<Double>
        get() = DIFs

    val macd: List<Double>
        get() = MACDs

    init {
        DEAs = ArrayList()
        DIFs = ArrayList()
        MACDs = ArrayList()

        val dEAs = ArrayList<Double>()
        val dIFs = ArrayList<Double>()
        val mACDs = ArrayList<Double>()

        var eMA12 = 0.0
        var eMA26 = 0.0
        var close = 0.0
        var dIF = 0.0
        var dEA = 0.0
        var mACD = 0.0
        if (kLineBeen != null && kLineBeen.size > 0) {
            for (i in kLineBeen.indices) {
                close = kLineBeen[i].close ?: 0.0
                if (i == 0) {
                    eMA12 = close
                    eMA26 = close
                } else {
                    eMA12 = eMA12 * 11 / 13 + close * 2 / 13
                    eMA26 = eMA26 * 25 / 27 + close * 2 / 27
                }
                dIF = eMA12 - eMA26
                dEA = dEA * 8 / 10 + dIF * 2 / 10
                mACD = dIF - dEA
                dEAs.add(dEA)
                dIFs.add(dIF)
                mACDs.add(mACD)
            }

            for (i in dEAs.indices) {
                DEAs.add(dEAs[i])
                DIFs.add(dIFs[i])
                MACDs.add(mACDs[i])
            }

        }

    }

}

