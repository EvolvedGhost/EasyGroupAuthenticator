package com.evolvedghost.command

import com.evolvedghost.EasyGroupAuthenticator
import com.evolvedghost.EasyGroupAuthenticator.reload
import com.evolvedghost.EasyGroupAuthenticator.save
import com.evolvedghost.config.EGAConfig
import com.evolvedghost.utils.EGAFunction
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.GroupAwareCommandSender
import net.mamoe.mirai.message.data.MessageChain

object EGACommand : CompositeCommand(
    EasyGroupAuthenticator,
    primaryName = "ega",
    secondaryNames = arrayOf("群验证"),
    description = "群验证管理指令",
) {
    @SubCommand("reload", "重载")
    @Description("重载本插件的配置项")
    suspend fun reload(sender: CommandSender) {
        EGAConfig.reload()
        EGAConfig.save()
        sender.sendMessage("重载完成")
    }

    @SubCommand("enable", "启用")
    @Description("开关本群的插件功能，关闭将取消本群已存在的所有验证")
    suspend fun enable(sender: CommandSender) {
        EGAFunction.enablePlugin(sender)
    }

    @SubCommand("auth", "验证码")
    @Description("更改本群验证码方式")
    suspend fun auth(sender: CommandSender, method: Int) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.changeAuthMethod(sender, sender.group.id, method)
    }

    @SubCommand("authChance", "验证码次数")
    @Description("更改本群验证码尝试次数")
    suspend fun authChance(sender: CommandSender, chance: Int) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.changeAuthChance(sender, sender.group.id, chance)
    }

    @SubCommand("authTime", "验证码时间")
    @Description("更改本群验证码超时时间（秒）")
    suspend fun authTime(sender: CommandSender, time: Int) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.changeAuthTime(sender, sender.group.id, time)
    }

    @SubCommand("levelLimit", "加群等级限制")
    @Description("更改本群加群等级限制，-1不限制")
    suspend fun levelLimit(sender: CommandSender, level: Int) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.changeLevelLimit(sender, sender.group.id, level)
    }

    @SubCommand("request", "进群审核")
    @Description("开关本群机器人自动进群审核")
    suspend fun request(sender: CommandSender, method: Int) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.changeRequestMethod(sender, sender.group.id, method)
    }

    @SubCommand("keywordAdd", "关键词增加")
    @Description("增加本群机器人自动进群审核通过关键词")
    suspend fun keywordAdd(sender: CommandSender, keyword: String) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.requestKeywordAdd(sender, sender.group.id, keyword, false)
    }

    @SubCommand("nKeywordAdd", "屏蔽词增加")
    @Description("增加本群机器人自动审核拒绝的关键词")
    suspend fun nKeywordAdd(sender: CommandSender, keyword: String) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.requestKeywordAdd(sender, sender.group.id, keyword, true)
    }

    @SubCommand("keywordRemove", "关键词移除")
    @Description("移除本群机器人自动进群审核通过关键词，输入*移除全部")
    suspend fun keywordRemove(sender: CommandSender, keyword: String) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.requestKeywordRemove(sender, sender.group.id, keyword, false)
    }

    @SubCommand("nKeywordRemove", "屏蔽词移除")
    @Description("移除本群机器人自动审核拒绝的关键词，输入*移除全部")
    suspend fun nKeywordRemove(sender: CommandSender, keyword: String) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.requestKeywordRemove(sender, sender.group.id, keyword, true)
    }

    @SubCommand("blackListAdd", "黑名单增加")
    @Description("增加禁入本群的用户")
    suspend fun blackListAdd(sender: CommandSender, qq: Long) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.blackListAdd(sender, sender.group.id, qq)
    }

    @SubCommand("blackListRemove", "黑名单移除")
    @Description("解禁被禁入本群的用户，输入*解禁全部")
    suspend fun blackListRemove(sender: CommandSender, qq: String) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.blackListRemove(sender, sender.group.id, qq)
    }

    @SubCommand("welcome", "欢迎信息")
    @Description("开启并编辑本群新人欢迎信息")
    suspend fun welcomeMessage(sender: CommandSender, vararg message: MessageChain) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.welcomeMessage(sender, sender.group.id, message)
    }

    @SubCommand("welcomeOff", "关闭欢迎信息")
    @Description("关闭本群新人欢迎信息")
    suspend fun welcomeMessageClear(sender: CommandSender) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.welcomeMessageClear(sender, sender.group.id)
    }

    @SubCommand("memberChange", "变动提醒")
    @Description("开关本群人员变动的提示信息")
    suspend fun exitNotify(sender: CommandSender) {
        if (!EGAFunction.isGroupMessage(sender)) return
        sender as GroupAwareCommandSender
        EGAFunction.memberChangeNotify(sender, sender.group.id)
    }
}