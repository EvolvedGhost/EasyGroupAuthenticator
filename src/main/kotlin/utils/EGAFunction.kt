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

/**
 * 插件核心功能方法集合。
 *
 * 提供配置读取、指令处理、权限校验等工具方法，供 [EGACommand] 和各事件监听器调用。
 */
object EGAFunction {

    // ======================== 配置读取 ========================

    /**
     * 读取指定群的验证配置。
     * 若该群尚无配置，则自动生成默认配置并写入 [EGAConfig]。
     *
     * @param groupId 群号
     * @return 该群的 [GroupAuthSetting] 配置
     */
    fun readGroupAuthSetting(groupId: Long): GroupAuthSetting {
        return EGAConfig.groupSettings.getOrPut(groupId) { GroupAuthSetting() }
    }

    // ======================== 权限与前置校验 ========================

    /**
     * 启用/关闭本群的验证功能（切换式开关）。
     *
     * 关闭时会取消该群所有进行中的验证任务。
     * 需要机器人为群管理员才能操作。
     */
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
            // 关闭：取消所有进行中的验证
            authData.cancel(sender.group.id)
            sender.sendMessage("已关闭本群群验证功能，所有正存在的验证都将被取消")
        } else {
            sender.sendMessage("已开启本群群验证功能")
        }
        config.enable = !config.enable
    }

    /**
     * 群指令通用前置校验：是否为群聊、机器人是否为管理员、插件是否已启用。
     *
     * @return true 表示校验通过，可继续执行指令逻辑；false 表示校验未通过（已自动回复原因）
     */
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

    // ======================== 验证码设置 ========================

    /**
     * 验证码类型编号到中文描述的映射。
     * @return 对应的中文描述，若编号无效则返回 null
     */
    private fun getMethodText(method: Int): String? = when (method) {
        0 -> "关闭验证码"
        1 -> "英文验证码"
        2 -> "英文动态验证码"
        3 -> "中文验证码"
        4 -> "中文动态验证码"
        5 -> "简单算术验证码"
        6 -> "困难算术验证码"
        else -> null
    }

    /** 更改本群的验证码类型 */
    suspend fun changeAuthMethod(sender: CommandSender, groupId: Long, method: Int) {
        val config = readGroupAuthSetting(groupId)
        val methodText = getMethodText(method)
        if (methodText == null) {
            // 编号无效时输出可选值列表
            val help = buildString {
                append("method可以设置为以下值：\n")
                for (i in 0..6) {
                    append(i).append(" > ").append(getMethodText(i)).append("\n")
                }
            }
            sender.sendMessage(help)
        } else {
            config.authMethod = method
            sender.sendMessage("已将本群验证方式设置为$methodText")
        }
    }

    /** 更改本群的验证码尝试次数 */
    suspend fun changeAuthChance(sender: CommandSender, groupId: Long, chance: Int) {
        val config = readGroupAuthSetting(groupId)
        if (chance <= 0) {
            sender.sendMessage("验证次数不可小于等于0")
        } else {
            config.authChance = chance
            sender.sendMessage("已将本群验证次数设置为 $chance 次")
        }
    }

    /** 更改本群的验证码超时时间（秒） */
    suspend fun changeAuthTime(sender: GroupAwareCommandSender, groupId: Long, time: Int) {
        val config = readGroupAuthSetting(groupId)
        if (time <= 0) {
            sender.sendMessage("验证时间不可小于等于0")
        } else {
            config.authTime = time
            sender.sendMessage("已将本群验证时间设置为 $time 秒")
        }
    }

    /** 更改本群的加群 QQ 等级限制（-1 为不限制） */
    suspend fun changeLevelLimit(sender: GroupAwareCommandSender, groupId: Long, level: Int) {
        val config = readGroupAuthSetting(groupId)
        config.levelLimit = level
        sender.sendMessage("已将本群进群等级限制为 $level 级")
    }

    // ======================== 自动审核设置 ========================

    /**
     * 自动审核方式编号到中文描述的映射。
     * @return 对应的中文描述，若编号无效则返回 null
     */
    private fun getRequestText(method: Int): String? = when (method) {
        0 -> "关闭自动审批"
        1 -> "模糊自动审批"
        2 -> "精确自动审批"
        else -> null
    }

    /** 更改本群的自动审核方式 */
    suspend fun changeRequestMethod(sender: CommandSender, groupId: Long, method: Int) {
        val config = readGroupAuthSetting(groupId)
        val methodText = getRequestText(method)
        if (methodText == null) {
            // 编号无效时输出可选值列表
            val help = buildString {
                append("method可以设置为以下值：\n")
                for (i in 0..2) {
                    append(i).append(" > ").append(getRequestText(i)).append("\n")
                }
            }
            sender.sendMessage(help)
        } else {
            config.requestMethod = method
            sender.sendMessage("已将本群验证方式设置为$methodText")
        }
    }

    /**
     * 添加审核关键词或屏蔽词。
     *
     * @param isNegative true 添加屏蔽词（命中则拒绝），false 添加通过关键词（命中则通过）
     */
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

    /**
     * 移除审核关键词或屏蔽词。传入 "*" 可清空全部。
     *
     * @param isNegative true 操作屏蔽词列表，false 操作通过关键词列表
     */
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

    // ======================== 黑名单管理 ========================

    /** 将指定 QQ 号加入本群黑名单 */
    suspend fun blackListAdd(sender: CommandSender, groupId: Long, qq: Long) {
        val config = readGroupAuthSetting(groupId)
        config.blackList.add(qq)
        sender.sendMessage("已添加黑名单用户：$qq")
    }

    /** 将指定 QQ 号从本群黑名单移除。传入 "*" 可清空全部黑名单 */
    suspend fun blackListRemove(sender: CommandSender, groupId: Long, qq: String) {
        val config = readGroupAuthSetting(groupId)
        if (qq == "*") {
            config.blackList.clear()
            sender.sendMessage("已移除全部黑名单用户")
        } else {
            config.blackList.remove(qq.toLong())
            sender.sendMessage("已移除黑名单用户：$qq")
        }
    }

    // ======================== 欢迎消息管理 ========================

    /**
     * 设置并开启本群新人欢迎消息。
     *
     * - 若 [message] 为空，则仅展示当前已设置的欢迎消息
     * - 若 [message] 非空，将其序列化为 MiraiCode 格式保存
     */
    suspend fun welcomeMessage(sender: CommandSender, groupId: Long, message: Array<out MessageChain>) {
        val config = readGroupAuthSetting(groupId)
        config.welcomeSwitch = true

        if (message.isEmpty()) {
            // 未传参：展示当前欢迎消息
            sender.sendMessage(buildMessageChain {
                +PlainText("已设置本群新人欢迎信息：\n")
                +config.welcomeMessage.deserializeMiraiCode()
            })
            return
        }

        // 将多条消息链拼接为 MiraiCode 字符串保存
        val miraiCode = message.joinToString("\n") { it.serializeToMiraiCode() }
        config.welcomeMessage = miraiCode

        sender.sendMessage(buildMessageChain {
            +PlainText("已设置本群新人欢迎信息：\n")
            +config.welcomeMessage.deserializeMiraiCode()
        })
    }

    /** 关闭本群新人欢迎消息 */
    suspend fun welcomeMessageClear(sender: CommandSender, groupId: Long) {
        val config = readGroupAuthSetting(groupId)
        config.welcomeSwitch = false
        sender.sendMessage("已关闭本群新人欢迎")
    }

    // ======================== 成员变动提醒 ========================

    /** 切换本群成员变动（进群/退群）提醒的开关 */
    suspend fun memberChangeNotify(sender: CommandSender, groupId: Long) {
        val config = readGroupAuthSetting(groupId)
        config.memberChange = !config.memberChange
        if (config.memberChange) {
            sender.sendMessage("已开启本群人员变化提醒")
        } else {
            sender.sendMessage("已关闭本群人员变化提醒")
        }
    }

    // ======================== QQ 等级校验 ========================

    /**
     * 检查新成员的 QQ 等级是否满足进群要求。
     *
     * 若等级低于 [config.levelLimit]，将自动踢出该成员并发送通知。
     *
     * @return true 表示等级不达标已被踢出（后续流程应终止），false 表示检查通过
     */
    suspend fun checkLevel(event: MemberJoinEvent, config: GroupAuthSetting): Boolean {
        val level = event.member.queryProfile().qLevel
        if (config.levelLimit > level) {
            event.group.sendMessage(
                "${event.member.nameCardOrNick}[${event.member.id}] 的加群请求因QQ等级小于${config.levelLimit}被踢出"
            )
            event.member.kick("本群不欢迎${level}级以下QQ加入")
            return true
        }
        return false
    }
}
