package com.linked.remoteadb

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)

//        StunClient.startRequest {
//            println(it)
//        }

        Thread.sleep(10000)
    }

    @Test
    fun addition_isCorrect222() {
        val c= InetAddress.getAllByName("stun.l.google.com")
        c.forEach {
            println("====${it.hostAddress},${it.javaClass}")
        }
    }
}