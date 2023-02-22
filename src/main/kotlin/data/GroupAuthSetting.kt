package com.evolvedghost.data

import kotlinx.serialization.Serializable

@Serializable
data class GroupAuthSetting(
    var enable: Boolean = false,
    var authMethod: Int = 0,
    var authChance: Int = 3,
    var authTime: Int = 300,
    var requestMethod: Int = 0,
    var requestKeywords: MutableList<String> = mutableListOf(),
    var requestNegativeKeywords: MutableList<String> = mutableListOf(),
    // var isBlackListWhiteList: Boolean = false,
    var blackList: MutableList<Long> = mutableListOf(),
    var welcomeSwitch: Boolean = false,
    var welcomeMessage: String = "进群欢迎信息",
    var memberChange: Boolean = false
)
