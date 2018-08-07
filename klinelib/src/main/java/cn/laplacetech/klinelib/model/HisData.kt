package cn.laplacetech.klinelib.model

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 * chart data model
 */
class HisData {

    var date: Long = 0
    var close: Double? = null
    var high: Double? = null
    var low: Double? = null
    var open: Double? = null
    var vol: Double? = null
    var amountVol: Double? = null

    //besttoken
    var ma5: Double? = null
    var ma10: Double? = null
    var ma20: Double? = null
    var ma30: Double? = null
    var ma60: Double? = null
    var ma120: Double? = null
    var change: Double? = null
    var change_ratio: Double? = null
    var volume_ma5: Double? = null
    var volume_ma10: Double? = null


    var avePrice: Double? = null
    var total: Double? = null
    var maSum: Double? = null


    var dif: Double? = null
    var dea: Double? = null
    var macd: Double? = null

    var k: Double? = null
    var d: Double? = null
    var j: Double? = null


    constructor() {}
    constructor(result: Array<Double>?) {
        if (result != null && result.size >= 17) {
            date = result[0].toLong()
            open = result[1]
            close = result[2]
            low = result[3]
            high = result[4]
            vol = result[5]
            amountVol = result[6]
            change = result[7]
            change_ratio = result[8]
            ma5 = result[9]
            ma10 = result[10]
            ma20 = result[11]
            ma30 = result[12]
            ma60 = result[13]
            ma120 = result[14]
            volume_ma5 = result[15]
            volume_ma10 = result[16]
        }
    }

    constructor(open: Double?, close: Double?, high: Double?, low: Double?, vol: Double?, date: Long) {
        this.open = open
        this.close = close
        this.high = high
        this.low = low
        this.vol = vol
        this.date = date
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val data = o as HisData?

        return date == data!!.date
    }

    override fun hashCode(): Int {
        return (date xor date.ushr(32)).toInt()
    }

    override fun toString(): String {
        return "HisData{" +
                "close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", open=" + open +
                ", vol=" + vol +
                ", date=" + date +
                ", amountVol=" + amountVol +
                ", avePrice=" + avePrice +
                ", total=" + total +
                ", maSum=" + maSum +
                ", ma5=" + ma5 +
                ", ma10=" + ma10 +
                ", ma20=" + ma20 +
                ", ma30=" + ma30 +
                ", dif=" + dif +
                ", dea=" + dea +
                ", macd=" + macd +
                ", k=" + k +
                ", d=" + d +
                ", j=" + j +
                '}'.toString()
    }

}
