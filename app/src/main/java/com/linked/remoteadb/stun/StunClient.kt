package com.linked.remoteadb.stun

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import org.ice4j.Transport
import org.ice4j.TransportAddress
import org.ice4j.ice.Agent
import org.ice4j.ice.CandidateType
import org.ice4j.ice.KeepAliveStrategy
import org.ice4j.ice.NominationStrategy
import org.ice4j.ice.harvest.StunCandidateHarvester
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds


/**
 *
 * ["stun:stun.miwifi.com","stun:stun.qq.com:3478","stun:stun.qq.com","stun:hw-v2-web-player-tracker.biliapi.net","stun:stun.smartgslb.com:19302","stun:stun.l.google.com:19302","stun:stun2.l.google.com:19302","stun:stun3.l.google.com:19302","stun:stun4.l.google.com:19302","stun:stun.nextcloud.com:443","stun:stun.cloudflare.com","stun:stunserver.stunprotocol.org:3478","stun:freestun.net:5350","stun:global.stun.twilio.com","stun:jp1.stun.twilio.com","stun:sg1.stun.twilio.com","stun:us1.stun.twilio.com","stun:stun.syncthing.net"]
 * */

object StunClient {

    init {
        System.setProperty("org.ice4j.ice.harvest.DISABLE_AWS_HARVESTER", "true")
        System.setProperty("org.ice4j.ice.harvest.HARVESTING_TIMEOUT", "90")
//        System.setProperty(
//            "org.ice4j.ice.harvest.STUN_MAPPING_HARVESTER_ADDRESSES",
//            "stun.cloudflare.com:3478"
//        )
    }

    private const val CDN_REQUEST_TIMEOUT = 30

    private const val STUN_DEFAULT_PORT = 3478

    private val stunServerList = listOf(
        "stun.cloudflare.com" to STUN_DEFAULT_PORT,
//        "stun:stun.cloudflare.com" to STUN_DEFAULT_PORT,
//        "stunserver.stunprotocol.org" to STUN_DEFAULT_PORT,
//        "stun.l.google.com" to 19302,
//        "stun.miwifi.com" to STUN_DEFAULT_PORT,
//        "stun.qq.com" to STUN_DEFAULT_PORT,
//        "hw-v2-web-player-tracker.biliapi.net" to STUN_DEFAULT_PORT,
//        "stun.smartgslb.com" to 19302,
    )

    private val executor by lazy {
        Executors.newSingleThreadExecutor()
    }

    private var agent: Agent? = null

    private val requestStart = AtomicBoolean(false)
    private val requestFinish = AtomicBoolean(false)
    private val candidate = LinkedHashMap<String, IpPenetrationInfo>()
    private val waitList: ArrayList<WaitTask> = ArrayList()

    fun startRequest(key: String = "", callback: (Map<String, IpPenetrationInfo>) -> Unit) {
        if (requestStart.compareAndSet(false, true)) {
            executor.execute {
                try {
                    addWaitingTask(key, callback)
                    createAgent().startCandidateTrickle {
                        if (requestFinish.get()) return@startCandidateTrickle
                        if (it == null) {
                            release()
                            invokeWaitingTask()
                        } else {
                            it.forEach { item ->
                                if (item.type in listOf(
                                        CandidateType.STUN_CANDIDATE,
                                        CandidateType.PEER_REFLEXIVE_CANDIDATE,
                                        CandidateType.SERVER_REFLEXIVE_CANDIDATE,
                                )) {
                                    addIp(
                                        item.stunServerAddress.hostAddress ?: "",
                                        item.reflexiveAddress.hostAddress ?: ""
                                    )
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    release()
                    invokeWaitingTask()
                }
            }
        } else {
            if (requestFinish.get()) {
                executor.execute {
                    callback.invoke(candidate)
                }
            } else {
                addWaitingTask(key, callback)
            }
        }
    }

    private fun addWaitingTask(key: String, callback: (Map<String, IpPenetrationInfo>) -> Unit) {
        synchronized(StunClient) {
            if (requestFinish.get()) {
                executor.execute {
                    callback.invoke(candidate)
                }
                return
            }
            if (key.isNotEmpty()) {
                waitList.removeIf { it.key == key }
            }
            waitList.add(WaitTask(key, callback))
        }
    }

    private fun invokeWaitingTask() {
        synchronized(StunClient) {
            waitList.forEach {
                it.callback.invoke(candidate)
            }
            waitList.clear()
        }
    }

    private fun createAgent(): Agent {
        val serverList = ArrayList<Pair<String, Int>>()
        val ipList = collectIpAddress()
        stunServerList.forEach { pair ->
            if (pair.first.isNotEmpty()) {
                val resolve = ipList.find { it.first == pair.first }
                if (resolve != null) {
                    //添加 ipv4 地址
                    if (resolve.second.isNotEmpty()) {
                        serverList.add(Pair(resolve.second, pair.second))
                    }
                    //添加 ipv6 地址
                    if (resolve.third.isNotEmpty()) {
                        serverList.add(Pair(resolve.third, pair.second))
                    }
                } else {
                    serverList.add(pair)
                }
            }
        }
        return Agent().apply {
            //setUseDynamicPorts(false)
            isTrickling = true
            nominationStrategy = NominationStrategy.NONE
            isControlling = false
            serverList.forEach {
                addCandidateHarvester(StunCandidateHarvester(TransportAddress(it.first, it.second, Transport.UDP)))
            }
            val stream = createMediaStream("ip_penetration")
            createComponent(stream, KeepAliveStrategy.SELECTED_ONLY, true)

            this@StunClient.agent = this
        }
    }

    // 返回结果：域名,ipv4地址,ipv6地址
    @OptIn(FlowPreview::class)
    private fun collectIpAddress(): List<Triple<String, String, String>> {
        val result = ArrayList<Triple<String, String, String>>()
        try {
            runBlocking(Dispatchers.IO) {
                flow {
                    stunServerList.forEach { pair ->
                        if (pair.first.isNotEmpty()) {
                            kotlin.runCatching {
                                var ipV4: String = ""
                                var ipV6: String = ""
                                InetAddress.getAllByName(pair.first).forEach {
                                    if (it is Inet6Address) {
                                        ipV6 = it.hostAddress ?: ""
                                    }
                                    if (it is Inet4Address) {
                                        ipV4 = it.hostAddress ?: ""
                                    }
                                }
                                if (ipV4.isNotEmpty() || ipV6.isNotEmpty()) {
                                    result.add(Triple(pair.first, ipV4, ipV6))
                                }
                            }
                        }
                    }
                    emit(Unit)
                }.timeout(CDN_REQUEST_TIMEOUT.seconds).firstOrNull()
            }
        } catch (_: Exception) { }
        return result
    }

    private fun release() {
        requestFinish.set(true)
        executor.execute {
            kotlin.runCatching {
                agent?.free()
            }
            agent = null
        }
    }

    private fun addIp(stun: String, ip: String) {
        if (ip.isEmpty()) return
        val finalStun = stun.ifEmpty { "unknown" }
        synchronized(candidate) {
            val info = candidate.get(finalStun) ?: IpPenetrationInfo(finalStun).also {
                candidate.put(finalStun, it)
            }
            if (!info.ipList.contains(ip)) {
                info.ipList.add(ip)
            }
        }
    }

    data class WaitTask(
        val key: String,
        val callback: (Map<String, IpPenetrationInfo>) -> Unit
    )

}