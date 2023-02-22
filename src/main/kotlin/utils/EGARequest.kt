package com.evolvedghost.utils

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberJoinRequestEvent

val groupRequestListener = GlobalEventChannel.subscribeAlways<MemberJoinRequestEvent> { event ->
    try {
        val config = EGAFunction.readGroupAuthSetting(event.groupId)
        if(!config.enable) return@subscribeAlways
        if (config.requestMethod == 0) return@subscribeAlways
        else if (config.requestMethod == 1) {
            if (config.blackList.contains(event.fromId)) {
                event.reject(true, "你在本群黑名单")
                return@subscribeAlways
            }
            var iterator = config.requestKeywords.iterator()
            while (iterator.hasNext()) {
                val keyword = iterator.next()
                if (event.message.contains(keyword)) {
                    event.accept()
                    return@subscribeAlways
                }
            }
            iterator = config.requestNegativeKeywords.iterator()
            while (iterator.hasNext()) {
                val keyword = iterator.next()
                if (event.message.contains(keyword)) {
                    event.reject(false)
                    return@subscribeAlways
                }
            }
        } else if (config.requestMethod == 2) {
            if (config.blackList.contains(event.fromId)) {
                event.reject(true, "你在本群黑名单")
            } else if (config.requestKeywords.contains(event.message)) {
                event.accept()
            } else if (config.requestNegativeKeywords.contains(event.message)) {
                event.reject(false)
            }
        }
        event.ignore()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}