package cn.laplacetech.klinelib.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * Created by lilifeng@laplacetech.cn on 2018/8/6.
 *
 */
object DoubleUtil {

    fun parseDouble(parserDouble: String): Double {
        try {
            return java.lang.Double.parseDouble(parserDouble)
        } catch (e: Exception) {
            return 0.0
        }

    }

    fun format2Decimal(d: Double?): String {
        val instance = DecimalFormat.getInstance()
        instance.minimumFractionDigits = 2
        instance.maximumFractionDigits = 2
        return instance.format(d)
    }

    fun formatDecimal(d: Double?): String {
        val instance = DecimalFormat.getInstance()
        instance.minimumFractionDigits = 0
        instance.maximumFractionDigits = 8
        return instance.format(d).replace(",", "")
    }


    /**
     * converting a double number to string by digits
     */
    fun getStringByDigits(num: Double, digits: Int): String {
        /* if (digits == 0) {
            return (int) num + "";
        } else {
            NumberFormat instance = DecimalFormat.getInstance();
            instance.setMinimumFractionDigits(digits);
            instance.setMaximumFractionDigits(digits);
            return instance.format(num).replace(",", "");
        }*/
        return String.format(Locale.getDefault(), "%." + digits + "f", num)
    }

    private const val MILLION = 10000.0
    private const val MILLIONS = 1000000.0
    private const val BILLION = 100000000.0
    private const val MILLION_UNIT = "万"
    private const val BILLION_UNIT = "亿"


    /**
     * 将数字转换成以万为单位或者以亿为单位，因为在前端数字太大显示有问题
     */
    fun amountConversion(amount: Double, keepTwoDigits: Boolean): String {
        val result: String
        result = if (amount < 0.001) {
            if ("$amount".length > 8) {
                "${formatDecimal(amount).toDouble()}"
            } else  {
                "$amount"
            }
        } else if (amount < 1 && !keepTwoDigits) {
            formatDecimal(amount)
        } else {
            val lengthInt = "${amount.toInt()}".length
            if (lengthInt < 8 && !keepTwoDigits) {
                getStringByDigits(amount, 8 - lengthInt)
            } else {
                formatNumberInfo(amount).toString()
            }
        }
//        while (result.contains('.')&& result.endsWith('0')){
//        }
        return result
    }

    /**
     * 将数字转换成以万为单位或者以亿为单位，因为在前端数字太大显示有问题
     *
     * @author
     * @version 1.00.00
     *
     * @date 2018年1月18日
     * @param amount 报销金额
     * @return
     */

    private fun formatNumberInfo(amount: Double): NumberInfo {
        var amount = amount
        val info = NumberInfo()
        if (amount == 0.0) {
            info.number = "0.00"
            info.unit = ""
            return info
        }
        val positiveNum = amount >= 0
        amount = Math.abs(amount)

        //最终返回的结果值
        info.number = amount.toString()
        //四舍五入后的值
        var value = 0.0
        //转换后的值
        var tempValue = 0.0
        //余数
        var remainder = 0.0

        //金额大于1万小于100万
        if (amount > MILLION && amount <= MILLIONS) {
            tempValue = amount / MILLION
            remainder = amount % MILLION

            //余数小于5000则不进行四舍五入
            //            if(remainder < (MILLION / 2)){
            //                value = formatNumber(tempValue,2,false);
            //            }else{
            //                value = formatNumber(tempValue,2,true);
            //            }
            value = formatNumber(tempValue, 2, true)

            info.number = zeroFill(value)
            info.unit = MILLION_UNIT
        } else if (amount > MILLIONS && amount <= BILLION) {
            tempValue = amount / MILLION
            remainder = amount % MILLION

            //余数小于5000则不进行四舍五入
            //            if(remainder < (MILLION / 2)){
            //                value = formatNumber(tempValue,2,false);
            //            }else{
            //                value = formatNumber(tempValue,2,true);
            //            }

            value = formatNumber(tempValue, 2, true)
            //如果值刚好是10000万，则要变成1亿
            if (value == MILLION) {
                info.number = zeroFill(value / MILLION)
                info.unit = BILLION_UNIT
            } else {
                info.number = zeroFill(value)
                info.unit = MILLION_UNIT
            }
        } else if (amount > BILLION) {
            tempValue = amount / BILLION
            remainder = amount % BILLION

            //余数小于50000000则不进行四舍五入
            //            if(remainder < (BILLION/2)){
            //                value = formatNumber(tempValue,2,false);
            //            }else{
            //                value = formatNumber(tempValue,2,true);
            //            }

            value = formatNumber(tempValue, 2, true)
            info.number = zeroFill(value)
            info.unit = BILLION_UNIT
        } else {
            value = formatNumber(amount, 2, true)
            info.number = zeroFill(value)
            info.unit = ""
        }//金额大于1亿
        //金额大于1百万小于1亿
        if (!positiveNum) {
            val tmp = java.lang.Double.parseDouble(info.number) * -1
            info.number = zeroFill(tmp)
        }

        return info
    }

    /**
     * Double类型保留指定位数的小数，返回double类型（四舍五入） newScale 为指定的位数
     */


    /**
     * 对数字进行四舍五入，保留2位小数
     *
     * @author
     * @version 1.00.00
     *
     * @date 2018年1月18日
     * @param number 要四舍五入的数字
     * @param decimal 保留的小数点数
     * @param rounding 是否四舍五入
     * @return
     */
    fun formatNumber(number: Double, decimal: Int, rounding: Boolean): Double {

        if (number.isNaN()) {
            return BigDecimal(0).setScale(decimal, RoundingMode.HALF_UP).toDouble()
        }
        val bigDecimal = BigDecimal(number)
        return if (rounding) {
            bigDecimal.setScale(decimal, RoundingMode.HALF_UP).toDouble()
        } else {
            bigDecimal.setScale(decimal, RoundingMode.DOWN).toDouble()
        }
    }

    /**
     * 对四舍五入的数据进行补0显示，即显示.00
     *
     * @author
     * @version 1.00.00
     *
     * @date 2018年1月23日
     * @return
     */
    fun zeroFill(number: Double): String {
        var value = number.toString()

        if (value.indexOf(".") < 0) {
            value += ".00"
        } else {
            val decimalValue = value.substring(value.indexOf(".") + 1)

            if (decimalValue.length < 2) {
                value += "0"
            }
        }
        return value
    }

    class NumberInfo {
        var number: String? = null
        var unit: String? = null

        override fun toString(): String {
            return number!! + unit!!
        }
    }


}
