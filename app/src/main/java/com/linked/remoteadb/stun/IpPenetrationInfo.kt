package com.linked.remoteadb.stun

data class IpPenetrationInfo(
    val stun: String,
    val ipList: ArrayList<String> = arrayListOf(),//反射ip对应stun服务器的url,多个服务器返回同一ip，按返回先后顺序排序
)