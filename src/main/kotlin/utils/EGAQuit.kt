package com.evolvedghost.utils

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberLeaveEvent

/**
 * 成员主动退群监听器。
 *
 * 当群验证已启用且开启了成员变动提醒时，发送退群通知消息。
 */
val groupQuitListener = GlobalEventChannel.subscribeAlways<MemberLeaveEvent.Quit> { event ->
    try {
        val config = EGAFunction.readGroupAuthSetting(event.groupId)
        if (config.enable && config.memberChange) {
            event.group.sendMessage("${event.member.nameCardOrNick} [${event.member.id}] 离开了本群")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}