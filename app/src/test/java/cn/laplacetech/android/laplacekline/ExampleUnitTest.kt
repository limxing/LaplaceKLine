package cn.laplacetech.android.laplacekline

import cn.laplacetech.klinelib.util.DoubleUtil
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun main() {
        val value= 0.0002042
//        val value= 0.2321412346312
//        val value= 23214123463.012123
//        val value = 23463.02343242
//        val value= 365.023123123
        println(value)

        println(DoubleUtil.formatDecimal(value))
        val lengthInt = "${value.toInt()}".length

        println(DoubleUtil.getStringByDigits(value, 2))


        println(DoubleUtil.amountConversion(value,true))
        assertEquals(4, 2 + 2)

    }
}
