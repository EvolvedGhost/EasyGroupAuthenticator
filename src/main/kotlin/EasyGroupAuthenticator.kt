package com.evolvedghost

import com.evolvedghost.command.EGACommand
import com.evolvedghost.config.EGAConfig
import com.evolvedghost.utils.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.utils.info

/**
 * EasyGroupAuthenticator 插件主入口。
 *
 * 这是一个基于 Mirai Console 的 QQ 群验证插件，支持以下功能：
 * - 新成员入群验证码（多种验证码类型）
 * - 入群申请自动审核（关键词匹配）
 * - QQ 等级限制
 * - 黑名单管理
 * - 新人欢迎消息
 * - 成员变动提醒
 */
object EasyGroupAuthenticator : KotlinPlugin(
    JvmPluginDescription(
        id = "com.evolvedghost.ezgroupauth",
        name = "EasyGroupAuthenticator",
        version = "0.0.1",
    ) {
        author("EvolvedGhost")
        info("""一个基于Mirai的群验证插件""")
    }
) {

    /**
     * 所有事件监听器的统一管理列表。
     * 便于在 onEnable/onDisable 中批量启动和停止，避免逐个判断。
     */
    private val listeners: List<Listener<*>> by lazy {
        listOf(
            groupRequestListener,       // 入群申请自动审核
            groupAuthInviteListener,     // 被邀请进群验证
            groupAuthActiveListener,     // 主动进群验证
            groupQuitListener,           // 成员退群通知
            groupMessageListener,        // 群消息监听（验证码回复）
        )
    }

    override fun onEnable() {
        // 加载配置
        EGAConfig.reload()

        // 注册指令
        EGACommand.register()

        // 初始化验证数据管理器（触发 authData 的 init 块，启动超时检测协程）
        authData

        // 启动所有事件监听器
        listeners.forEach { listener ->
            if (!listener.isActive) listener.start()
        }

        logger.info { "EasyGroupAuthenticator - 群验证插件 加载完成" }
    }

    override fun onDisable() {
        // 保存配置
        EGAConfig.save()

        // 注销指令
        EGACommand.unregister()

        // 停止所有事件监听器
        listeners.forEach { listener ->
            if (listener.isActive) listener.complete()
        }

        super.onDisable()
    }
}
