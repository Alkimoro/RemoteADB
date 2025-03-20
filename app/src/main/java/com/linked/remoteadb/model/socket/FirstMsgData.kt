package com.linked.remoteadb.model.socket

import com.google.gson.annotations.SerializedName

data class FirstMsgData(
    @SerializedName("device_name")
    val deviceName: String,

    @SerializedName("device_model")
    val deviceModel: String,

    @SerializedName("os_version")
    val osVersion: String,
)
