package com.evolvedghost.utils

import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberLeaveEvent

val groupQuitListener = GlobalEventChannel.subscribeAlways<MemberLeaveEvent.Quit> { event ->
    try {
        val config = EGAFunction.readGroupAuthSetting(event.groupId)
        if(!config.enable && config.memberChange) return@subscribeAlways
        event.group.sendMessage("${event.member.nameCardOrNick} [${event.member.id}] 离开了本群")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}