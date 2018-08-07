package cn.laplacetech.klinelib.util

import cn.laplacetech.klinelib.model.HisData
import cn.laplacetech.klinelib.model.KDJ
import cn.laplacetech.klinelib.model.MACD

import java.util.ArrayList

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */

object DataUtils {


    /**
     * calculate average price and ma data
     */
    @JvmOverloads
    fun calculateHisData(list: List<HisData>, lastData: HisData? = null): List<HisData> {


        val ma5List = calculateMA(5, list)
        val ma10List = calculateMA(10, list)
        val ma20List = calculateMA(20, list)
        val ma30List = calculateMA(30, list)

        val maVol5List = calculateVolMA(5, list)
        val maVol10List = calculateVolMA(10, list)

        val macd = MACD(list)
        val bar = macd.macd
        val dea = macd.dea
        val dif = macd.dif
        val kdj = KDJ(list)
        val d = kdj.d
        val k = kdj.k
        val j = kdj.j

        var amountVol = lastData?.amountVol ?: 0.0
        for (i in list.indices) {
            val hisData = list[i]
            hisData.ma5 = ma5List[i]
            hisData.ma10 = ma10List[i]
            hisData.ma20 = ma20List[i]
            hisData.ma30 = ma30List[i]

            hisData.volume_ma5 = maVol5List[i]
            hisData.volume_ma10 = maVol10List[i]



            hisData.macd = bar[i]
            hisData.dea = dea[i]
            hisData.dif = dif[i]

            hisData.d = d[i]
            hisData.k = k[i]
            hisData.j = j[i]

            amountVol += hisData.vol!!
            hisData.amountVol = amountVol
            when {
                i > 0 -> {
                    val total = hisData.vol!! * hisData.close!! + list[i - 1].total!!
                    hisData.total = total
                    val avePrice = total / amountVol
                    hisData.avePrice = avePrice
                }
                lastData != null -> {
                    val total = hisData.vol!! * hisData.close!! + lastData.total!!
                    hisData.total = total
                    val avePrice = total / amountVol
                    hisData.avePrice = avePrice
                }
                else -> {
                    hisData.amountVol = hisData.vol
                    hisData.avePrice = hisData.close
                    hisData.total = hisData.amountVol!! * hisData.avePrice!!
                }
            }

        }
        return list
    }


    /**
     * according to the history data list, calculate a new data
     */
    fun calculateHisData(newData: HisData, hisDatas: List<HisData>): HisData {

        val lastData = hisDatas[hisDatas.size - 1]
        var amountVol = lastData.amountVol!!

        newData.ma5 = calculateLastMA(5, hisDatas)
        newData.ma10 = calculateLastMA(10, hisDatas)
        newData.ma20 = calculateLastMA(20, hisDatas)
        newData.ma30 = calculateLastMA(30, hisDatas)

        amountVol += newData.vol!!
        newData.amountVol = amountVol

        val total = newData.vol!! * newData.close!! + lastData.total!!
        newData.total = total
        val avePrice = total / amountVol
        newData.avePrice = avePrice

        val macd = MACD(hisDatas)
        val bar = macd.macd
        newData.macd = bar[bar.size - 1]
        val dea = macd.dea
        newData.dea = dea[dea.size - 1]
        val dif = macd.dif
        newData.dif = dif[dif.size - 1]
        val kdj = KDJ(hisDatas)
        val d = kdj.d
        newData.d = d[d.size - 1]
        val k = kdj.k
        newData.k = k[k.size - 1]
        val j = kdj.j
        newData.j = j[j.size - 1]

        return newData
    }

    /**
     * calculate VolMA value return double list
     */
    private fun calculateVolMA(i: Int, data: List<HisData>): List<Double> {
        var dayCount = i
        dayCount--
        val result = ArrayList<Double>(data.size)
        var i = 0
        val len = data.size
        while (i < len) {
            if (i < dayCount) {
                result.add(java.lang.Double.NaN)
                i++
                continue
            }
            val sum = (0 until dayCount).sumByDouble { data[i - it].vol!! }
            result.add(+(sum / dayCount))
            i++
        }
        return result

    }

    /**
     * calculate MA value, return a double list
     * @param dayCount for example: 5, 10, 20, 30
     */
    fun calculateMA(dayCount: Int, data: List<HisData>): List<Double> {
        var dayCount = dayCount
        dayCount--
        val result = ArrayList<Double>(data.size)
        var i = 0
        val len = data.size
        while (i < len) {
            if (i < dayCount) {
                result.add(java.lang.Double.NaN)
                i++
                continue
            }
            val sum = (0 until dayCount).sumByDouble { data[i - it].open!! }
            result.add(+(sum / dayCount))
            i++
        }
        return result
    }

    /**
     * calculate last MA value, return a double value
     */
    fun calculateLastMA(dayCount: Int, data: List<HisData>): Double {
        var dayCount = dayCount
        dayCount--
        var result = java.lang.Double.NaN
        var i = 0
        val len = data.size
        while (i < len) {
            if (i < dayCount) {
                result = java.lang.Double.NaN
                i++
                continue
            }
            var sum = 0.0
            for (j in 0 until dayCount) {
                sum += data[i - j].open!!
            }
            result = +(sum / dayCount)
            i++
        }
        return result
    }


}
