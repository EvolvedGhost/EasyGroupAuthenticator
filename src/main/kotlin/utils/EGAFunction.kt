package com.evolvedghost.utils

import com.evolvedghost.config.EGAConfig
import com.evolvedghost.data.GroupAuthSetting
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.GroupAwareCommandSender
import net.mamoe.mirai.contact.isOperator
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain

object EGAFunction {
    fun readGroupAuthSetting(groupId: Long): GroupAuthSetting {
        var config = EGAConfig.groupSettings[groupId]
        if (config == null) {
            config = GroupAuthSetting()
            EGAConfig.groupSettings[groupId] = config
        }
        return config
    }

    suspend fun enablePlugin(sender: CommandSender) {
        if (sender !is GroupAwareCommandSender) {
            sender.sendMessage("请在对应群聊环境执行")
            return
        }
        val config = readGroupAuthSetting(sender.group.id)
        if (!sender.group.botAsMember.isOperator()) {
            config.enable = false
            sender.sendMessage("机器人并非管理员")
            return
        }
        if (config.enable) {
            authData.cancel(sender.group.id)
            sender.sendMessage("已关闭本群群验证功能，所有正存在的验证都将被取消")
        } else {
            sender.sendMessage("已开启本群群验证功能")
        }
        config.enable = !config.enable
    }

    suspend fun isGroupMessage(sender: CommandSender): Boolean {
        if (sender !is GroupAwareCommandSender) {
            sender.sendMessage("请在对应群聊环境执行")
            return false
        }
        val config = readGroupAuthSetting(sender.group.id)
        if (!sender.group.botAsMember.isOperator()) {
            config.enable = false
            sender.sendMessage("机器人并非管理员")
            return false
        }
        if (!config.enable) {
            sender.sendMessage("本群未启用")
            return false
        }
        return true
    }

    private fun getMethodText(method: Int): String? {
        return when (method) {
            0 -> "关闭验证码"
            1 -> "英文验证码"
            2 -> "英文动态验证码"
            3 -> "中文验证码"
            4 -> "中文动态验证码"
            5 -> "简单算术验证码"
            6 -> "困难算术验证码"
            else -> null
        }
    }

    suspend fun changeAuthMethod(sender: CommandSender, groupId: Long, method: Int) {
        val config = readGroupAuthSetting(groupId)
        val methodText = getMethodText(method)
        if (methodText == null) {
            val sb = StringBuilder("method可以设置为以下值：\n")
            for (i in 0 until 6) {
                sb.append(i)
                sb.append(" > ")
                sb.append(getMethodText(i))
                sb.append("\n")
            }
            sender.sendMessage(sb.toString())
        } else {
            config.authMethod = method
            sender.sendMessage("已将本群验证方式设置为$methodText")
        }
    }

    suspend fun changeAuthChance(sender: CommandSender, groupId: Long, chance: Int) {
        val config = readGroupAuthSetting(groupId)
        if (chance <= 0) {
            sender.sendMessage("验证次数不可小于等于0")
        } else {
            config.authChance = chance
            sender.sendMessage("已将本群验证次数设置为 $chance 次")
        }
    }

    suspend fun changeAuthTime(sender: GroupAwareCommandSender, groupId: Long, time: Int) {
        val config = readGroupAuthSetting(groupId)
        if (time <= 0) {
            sender.sendMessage("验证时间不可小于等于0")
        } else {
            config.authTime = time
            sender.sendMessage("已将本群验证次数设置为 $time 秒")
        }
    }

    suspend fun changeLevelLimit(sender: GroupAwareCommandSender, groupId: Long, level: Int) {
        val config = readGroupAuthSetting(groupId)
        config.levelLimit = level
        sender.sendMessage("已将本群进群等级限制为 $level 级")
    }

    private fun getRequestText(method: Int): String? {
        return when (method) {
            0 -> "关闭自动审批"
            1 -> "模糊自动审批"
            2 -> "精确自动审批"
            else -> null
        }
    }

    suspend fun changeRequestMethod(sender: CommandSender, groupId: Long, method: Int) {
        val config = readGroupAuthSetting(groupId)
        val methodText = getRequestText(method)
        if (methodText == null) {
            val sb = StringBuilder("method可以设置为以下值：\n")
            for (i in 0 until 2) {
                sb.append(i)
                sb.append(" > ")
                sb.append(getMethodText(i))
                sb.append("\n")
            }
            sender.sendMessage(sb.toString())
        } else {
            config.requestMethod = method
            sender.sendMessage("已将本群验证方式设置为$methodText")
        }
    }

    suspend fun requestKeywordAdd(sender: CommandSender, groupId: Long, keyword: String, isNegative: Boolean) {
        val config = readGroupAuthSetting(groupId)
        if (isNegative) {
            config.requestNegativeKeywords.add(keyword)
            sender.sendMessage("已添加屏蔽词：$keyword")
        } else {
            config.requestKeywords.add(keyword)
            sender.sendMessage("已添加关键词：$keyword")
        }
    }

    suspend fun requestKeywordRemove(sender: CommandSender, groupId: Long, keyword: String, isNegative: Boolean) {
        val config = readGroupAuthSetting(groupId)
        if (keyword == "*") {
            if (isNegative) {
                config.requestNegativeKeywords.clear()
                sender.sendMessage("已移除全部屏蔽词")
            } else {
                config.requestKeywords.clear()
                sender.sendMessage("已移除全部关键词")
            }
        } else {
            if (isNegative) {
                config.requestNegativeKeywords.remove(keyword)
                sender.sendMessage("已移除屏蔽词：$keyword")
            } else {
                config.requestKeywords.remove(keyword)
                sender.sendMessage("已移除关键词：$keyword")
            }
        }
    }

    suspend fun blackListAdd(sender: CommandSender, groupId: Long, qq: Long) {
        val config = readGroupAuthSetting(groupId)
        config.blackList.add(qq)
        sender.sendMessage("已添加黑名单用户：$qq")
    }

    suspend fun blackListRemove(sender: CommandSender, groupId: Long, qq: String) {
        val config = readGroupAuthSetting(groupId)
        if (qq == "*") {
            config.blackList.clear()
        } else {
            config.blackList.remove(qq.toLong())
            sender.sendMessage("已移除黑名单用户：$qq")
        }
    }

    suspend fun welcomeMessage(sender: CommandSender, groupId: Long, message: Array<out MessageChain>) {
        val config = readGroupAuthSetting(groupId)
        config.welcomeSwitch = true
        if (message.isEmpty()) {
            sender.sendMessage(buildMessageChain {
                +PlainText("已设置本群新人欢迎信息：\n")
                +config.welcomeMessage.deserializeMiraiCode()
            })
            return
        }
        val sb = StringBuilder()
        for (m in message) {
            sb.append(m.serializeToMiraiCode())
            sb.append("\n")
        }
        sb.deleteCharAt(sb.length - 1)
        config.welcomeMessage = sb.toString()
        sender.sendMessage(buildMessageChain {
            +PlainText("已设置本群新人欢迎信息：\n")
            +config.welcomeMessage.deserializeMiraiCode()
        })
    }

    suspend fun welcomeMessageClear(sender: CommandSender, groupId: Long) {
        val config = readGroupAuthSetting(groupId)
        config.welcomeSwitch = false
        sender.sendMessage("已关闭本群新人欢迎")
    }

    suspend fun memberChangeNotify(sender: CommandSender, groupId: Long) {
        val config = readGroupAuthSetting(groupId)
        if (config.memberChange) {
            sender.sendMessage("已关闭本群人员变化提醒")
        } else {
            sender.sendMessage("已开启本群人员变化提醒")
        }
        config.memberChange = !config.memberChange
    }

    suspend fun checkLevel(event: MemberJoinEvent, config: GroupAuthSetting): Boolean {
        val level = event.member.queryProfile().qLevel
        if (config.levelLimit > level) {
            event.group.sendMessage("${event.member.nameCardOrNick}[${event.member.id}] 的加群请求因QQ等级小于${config.levelLimit}被踢出")
            event.member.kick("本群不欢迎${level}级以下QQ加入")
            return true
        }
        return false
    }
}