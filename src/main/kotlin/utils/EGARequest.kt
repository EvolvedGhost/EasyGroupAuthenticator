package com.evolvedghost.utils

import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberJoinRequestEvent

/**
 * 入群申请自动审核监听器。
 *
 * 根据群配置的 [requestMethod][com.evolvedghost.data.GroupAuthSetting.requestMethod] 自动处理入群申请：
 * - 0：关闭自动审核，不做任何处理
 * - 1：模糊匹配 — 申请消息**包含**任一关键词即通过，**包含**任一屏蔽词即拒绝
 * - 2：精确匹配 — 申请消息**完全等于**任一关键词即通过，**完全等于**任一屏蔽词即拒绝
 *
 * 黑名单用户无论哪种模式都会被直接拒绝并拉黑。
 */
val groupRequestListener = GlobalEventChannel.subscribeAlways<MemberJoinRequestEvent> { event ->
    try {
        val config = EGAFunction.readGroupAuthSetting(event.groupId)

        // 插件未启用或自动审核关闭时跳过
        if (!config.enable || config.requestMethod == 0) return@subscribeAlways

        // 黑名单检查（所有审核模式共用）
        if (config.blackList.contains(event.fromId)) {
            event.reject(true, "你在本群黑名单")
            return@subscribeAlways
        }

        when (config.requestMethod) {
            1 -> {
                // 模糊匹配：申请消息包含关键词即通过
                if (config.requestKeywords.any { event.message.contains(it) }) {
                    event.accept()
                    return@subscribeAlways
                }
                // 模糊匹配：申请消息包含屏蔽词即拒绝
                if (config.requestNegativeKeywords.any { event.message.contains(it) }) {
                    event.reject(false)
                    return@subscribeAlways
                }
            }
            2 -> {
                // 精确匹配：申请消息完全等于关键词即通过
                if (config.requestKeywords.contains(event.message)) {
                    event.accept()
                    return@subscribeAlways
                }
                // 精确匹配：申请消息完全等于屏蔽词即拒绝
                if (config.requestNegativeKeywords.contains(event.message)) {
                    event.reject(false)
                    return@subscribeAlways
                }
            }
        }

        // 未命中任何规则，忽略本次申请
        event.ignore()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
