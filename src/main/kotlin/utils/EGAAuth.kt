package com.evolvedghost.utils

import net.mamoe.mirai.contact.Contact.Companion.uploadImage
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.buildMessageChain

/**
 * 入群验证相关的两个事件监听器。
 *
 * - [groupAuthInviteListener]：处理被邀请进群的情况
 * - [groupAuthActiveListener]：处理主动申请进群的情况
 *
 * 两者共享相同的前置校验和验证码发放流程，差异仅在于消息文案。
 */

// ======================== 被邀请进群监听器 ========================

/** 监听被邀请进群事件：邀请入群后触发验证流程 */
val groupAuthInviteListener = GlobalEventChannel.subscribeAlways<MemberJoinEvent.Invite> { event ->
    try {
        handleMemberJoin(event) {
            // 构建邀请场景下的提示消息（包含邀请者信息）
            buildMessageChain {
                +event.invitor.at()
                +PlainText(" 邀请了")
                +event.member.at()
                +PlainText(" 加入本群")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ======================== 主动进群监听器 ========================

/** 监听主动进群事件：通过申请进群后触发验证流程 */
val groupAuthActiveListener = GlobalEventChannel.subscribeAlways<MemberJoinEvent.Active> { event ->
    try {
        handleMemberJoin(event) {
            // 构建主动进群场景下的提示消息（无邀请者信息）
            buildMessageChain {
                +event.member.at()
                +PlainText(" 加入本群")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ======================== 公共进群处理逻辑 ========================

/**
 * 处理新成员进群的验证流程（Invite 和 Active 共用）。
 *
 * 流程：
 * 1. 检查 QQ 等级限制
 * 2. 检查插件是否启用
 * 3. 检查黑名单
 * 4. 若验证码关闭（authMethod=0），仅发送进群通知（若开启了变动提醒）
 * 5. 若验证码开启，生成验证码图片、创建验证任务、发送提示
 *
 * @param event           入群事件
 * @param joinNoticeBuilder 构建进群通知消息的 lambda，不同进群方式消息不同
 */
private suspend fun handleMemberJoin(
    event: MemberJoinEvent,
    joinNoticeBuilder: () -> net.mamoe.mirai.message.data.MessageChain
) {
    val config = EGAFunction.readGroupAuthSetting(event.groupId)

    // 1. QQ 等级检查
    if (EGAFunction.checkLevel(event, config)) return

    // 2. 插件启用检查
    if (!config.enable) return

    // 3. 黑名单检查
    if (config.blackList.contains(event.member.id)) {
        event.member.kick("你在本群黑名单中", true)
        event.group.sendMessage("黑名单用户${event.member.id}进群，已被踢出")
        return
    }

    // 4. 验证码关闭时：仅发送进群通知（如果开启了成员变动提醒）
    if (config.authMethod == 0) {
        if (config.memberChange) {
            event.group.sendMessage(joinNoticeBuilder())
        }
        return
    }

    // 5. 生成验证码、创建验证任务、发送提示
    val captcha = EGACaptcha.getCaptcha(event.groupId)
    val image = event.group.uploadImage(captcha.image, captcha.format)

    authData.add(
        EGAAuthData.AuthInfo(
            event = event,
            botId = event.bot.id,
            groupId = event.groupId,
            userId = event.member.id,
            timeout = System.currentTimeMillis() + (config.authTime * 1000),
            answer = captcha.answer,
            chance = config.authChance
        )
    )

    event.group.sendMessage(buildMessageChain {
        +joinNoticeBuilder()
        +PlainText("，请输入以下验证码完成入群（如显示不全请点开，如为计算题请直接输入计算答案）：\n")
        +image
    })
}
