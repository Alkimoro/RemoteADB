package com.linked.remoteadb

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.linked.remoteadb", appContext.packageName)
    }
    fun hexStringToByteArray(hexString: String): ByteArray {
        val byteArray = ByteArray(hexString.length / 2)
        for (i in byteArray.indices) {
            val index = i * 2
            val hex = hexString.substring(index, index + 2)
            byteArray[i] = hex.toInt(16).toByte()
        }
        return byteArray
    }

    @Test
    fun test() {
        //byteArrayOf(0xE0.toByte(), 0x00, 0x00, 0x00)
        //'\x00\x00\x00\x00\x00\x03S\x02'
        val arr = hexStringToByteArray("0000000000035302")
        val s = "AICQAAzMAAAEAGAAAIR+X+//////9ELUQOwANcDB1gkwKUBJUGlgiXCpgMmQ6aEJsSnBSe"

        val sa = ByteBuffer.wrap(arr).order(ByteOrder.BIG_ENDIAN).getLong().toInt()
        println("===$sa")
    }
}