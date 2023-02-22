package com.evolvedghost

import com.evolvedghost.command.EGACommand
import com.evolvedghost.config.EGAConfig
import com.evolvedghost.utils.*
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.register
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.info

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
    override fun onEnable() {
        EGAConfig.reload()
        EGACommand.register()
        authData
        if (!groupRequestListener.isActive) {
            groupRequestListener.start()
        }
        if (!groupAuthInviteListener.isActive) {
            groupAuthInviteListener.start()
        }
        if (!groupAuthActiveListener.isActive) {
            groupAuthActiveListener.start()
        }
        if (!groupQuitListener.isActive) {
            groupQuitListener.start()
        }
        if (!groupMessageListener.isActive) {
            groupMessageListener.start()
        }
        logger.info { "EasyGroupAuthenticator - 群验证插件 加载完成" }
    }

    override fun onDisable() {
        EGAConfig.save()
        EGACommand.unregister()
        if (groupRequestListener.isActive) {
            groupRequestListener.complete()
        }
        if (groupAuthInviteListener.isActive) {
            groupAuthInviteListener.complete()
        }
        if (groupAuthActiveListener.isActive) {
            groupAuthActiveListener.complete()
        }
        if (groupQuitListener.isActive) {
            groupQuitListener.complete()
        }
        if (groupMessageListener.isActive) {
            groupMessageListener.complete()
        }
        super.onDisable()
    }
}