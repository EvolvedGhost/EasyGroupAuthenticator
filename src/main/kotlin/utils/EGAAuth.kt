package com.evolvedghost.utils

import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.buildMessageChain

val groupAuthInviteListener = GlobalEventChannel.subscribeAlways<MemberJoinEvent.Invite> { event ->
    try {
        val config = EGAFunction.readGroupAuthSetting(event.groupId)
        if (!config.enable) return@subscribeAlways
        if (config.blackList.contains(event.member.id)) {
            event.member.kick("你在本群黑名单中", true)
            event.group.sendMessage("黑名单用户${event.member.id}进群，已被踢出")
            return@subscribeAlways
        } else if (config.authMethod == 0) {
            if (config.memberChange) {
                event.group.sendMessage(buildMessageChain {
                    +event.invitor.at()
                    +PlainText(" 邀请了")
                    +event.member.at()
                    +PlainText(" 加入本群")
                })
            }
            return@subscribeAlways
        }
        val captcha = EGACaptcha.getCaptcha(event.groupId)
        val image = event.group.uploadImage(captcha.image, captcha.format)
        authData.add(
            EGAAuthData.AuthInfo(
                event.bot.id,
                event.groupId,
                event.member.id,
                System.currentTimeMillis() + (config.authTime * 1000),
                captcha.answer,
                config.authChance
            )
        )
        event.group.sendMessage(buildMessageChain {
            +event.invitor.at()
            +PlainText(" 邀请了")
            +event.member.at()
            +PlainText(" 加入本群，请输入以下验证码完成入群：\n")
            +image
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

val groupAuthActiveListener = GlobalEventChannel.subscribeAlways<MemberJoinEvent.Active> { event ->
    try {
        val config = EGAFunction.readGroupAuthSetting(event.groupId)
        if(!config.enable) return@subscribeAlways
        if (config.blackList.contains(event.member.id)) {
            event.member.kick("你在本群黑名单中", true)
            event.group.sendMessage("黑名单用户${event.member.id}进群，已被踢出")
            return@subscribeAlways
        } else if (config.authMethod == 0) {
            if (config.memberChange) {
                event.group.sendMessage(buildMessageChain {
                    +event.member.at()
                    +PlainText(" 加入本群")
                })
            }
            return@subscribeAlways
        }
        val captcha = EGACaptcha.getCaptcha(event.groupId)
        val image = event.group.uploadImage(captcha.image, captcha.format)
        authData.add(
            EGAAuthData.AuthInfo(
                event.bot.id,
                event.groupId,
                event.member.id,
                System.currentTimeMillis() + (config.authTime * 1000),
                captcha.answer,
                config.authChance
            )
        )
        event.group.sendMessage(buildMessageChain {
            +event.member.at()
            +PlainText(" 加入本群，请输入以下验证码完成入群：\n")
            +image
        })
    } catch (e: Exception) {
        e.printStackTrace()
    }
}