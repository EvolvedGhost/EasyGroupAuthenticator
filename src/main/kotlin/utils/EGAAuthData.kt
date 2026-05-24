package com.evolvedghost.utils

import com.evolvedghost.config.EGAConfig
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.at
import net.mamoe.mirai.message.data.buildMessageChain

/** 全局验证数据管理器实例，在插件 onEnable 时通过访问此属性触发初始化 */
val authData = EGAAuthData()

/**
 * 群消息监听器：拦截群消息并检查是否为验证码回复。
 *
 * 当某群的某位成员正处于验证流程中时，其发送的群消息将被当作验证码答案进行校验。
 */
val groupMessageListener = GlobalEventChannel.subscribeAlways<GroupMessageEvent> { event ->
    try {
        if (authData.hasAuthTask(event.group.id, event.sender.id)) {
            // 仅提取 PlainText 内容作为验证码答案，避免 at、表情等元素干扰
            val plainText = event.message.filterIsInstance<PlainText>()
                .joinToString("") { it.content }
                .trim()
            if (plainText.isNotEmpty()) {
                authData.verifyAnswer(
                    event.group.id,
                    event.sender.id,
                    plainText
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// ======================== 验证数据管理 ========================

/**
 * 管理所有进行中的入群验证任务。
 *
 * - 内部使用 [Mutex] 保证并发安全
 * - 启动后台协程定期扫描并踢出超时未验证的成员
 * - 提供验证答案校验、验证成功/失败处理等核心逻辑
 */
@OptIn(DelicateCoroutinesApi::class)
class EGAAuthData {

    /**
     * 单条验证任务的信息。
     *
     * @property event   触发验证的入群事件（Invite 或 Active），用于后续踢出/发送消息
     * @property botId   机器人 QQ 号
     * @property groupId 群号
     * @property userId  待验证成员的 QQ 号
     * @property timeout 验证截止时间戳（毫秒），超过此时间视为超时
     * @property answer  验证码的正确答案
     * @property chance  剩余尝试次数，归零后踢出
     */
    data class AuthInfo(
        val event: MemberJoinEvent,
        val botId: Long,
        val groupId: Long,
        val userId: Long,
        val timeout: Long,
        val answer: String,
        var chance: Int,
    )

    /** 所有进行中的验证任务列表 */
    private val authTasks = mutableListOf<AuthInfo>()

    /** 保护 [authTasks] 的互斥锁，防止并发读写 */
    private val mutex = Mutex()

    init {
        // 启动后台超时检测协程：定期扫描所有验证任务，超时则踢出成员
        GlobalScope.launch {
            while (true) {
                delay(EGAConfig.timeoutDetectCycle * 1000)
                checkTimeout()
            }
        }
    }

    /**
     * 扫描所有验证任务，处理已超时的条目。
     * 超时成员将被踢出并收到提示消息。
     *
     * 将数据变更和 IO 操作分离：mutex 仅保护列表操作，消息发送在锁外执行，
     * 避免网络阻塞导致锁被长期持有从而影响其他操作。
     */
    private suspend fun checkTimeout() {
        val now = System.currentTimeMillis()
        // 在锁内收集并移除超时任务，拿到需要处理的任务副本
        val expired: List<AuthInfo>
        mutex.withLock {
            expired = authTasks.filter { now > it.timeout }
            for (info in expired) {
                removeAuthTask(info.groupId, info.userId)
            }
        }
        // 在锁外执行 IO 操作（发消息、踢人），避免阻塞锁
        for (info in expired) {
            try {
                handleTimeout(info)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ======================== 查询方法 ========================

    /**
     * 检查指定群的指定成员是否有进行中的验证任务。
     */
    fun hasAuthTask(groupId: Long, userId: Long): Boolean =
        authTasks.any { it.groupId == groupId && it.userId == userId }

    /**
     * 获取指定群的指定成员的验证任务信息。
     * @return 验证任务信息，若不存在则返回 null
     */
    private fun findAuthTask(groupId: Long, userId: Long): AuthInfo? =
        authTasks.find { it.groupId == groupId && it.userId == userId }

    // ======================== 验证逻辑 ========================

    /**
     * 校验成员提交的验证码答案。
     *
     * - 任务已超时：直接判定为超时踢出（不依赖后台协程，避免竞态）
     * - 答案正确：移除任务，发送欢迎消息
     * - 答案错误：扣减剩余机会，机会耗尽则踢出
     * - 任务不存在：忽略
     *
     * @param groupId 群号
     * @param userId  成员 QQ 号
     * @param answer  成员提交的验证码答案（已过滤为纯文本）
     */
    suspend fun verifyAnswer(groupId: Long, userId: Long, answer: String) {
        // action: 0=无需IO, 1=验证成功, 2=错误提示, 3=错误踢出, 4=超时踢出
        var action = 0
        var task: AuthInfo? = null

        mutex.withLock {
            val found = findAuthTask(groupId, userId)
            if (found == null) {
                // 任务不存在，无需处理
            } else if (System.currentTimeMillis() > found.timeout) {
                // 任务已超时：无论用户输入什么，都直接判定为超时踢出
                removeAuthTask(groupId, userId)
                task = found
                action = 4
            } else if (answer.equals(found.answer, ignoreCase = true)) {
                // 答案正确
                removeAuthTask(groupId, userId)
                task = found
                action = 1
            } else {
                // 答案错误：扣减机会
                found.chance--
                task = found
                action = if (found.chance <= 0) {
                    removeAuthTask(groupId, userId)
                    3
                } else {
                    2
                }
            }
        }

        // 在锁外执行 IO 操作
        when (action) {
            1 -> handleSuccess(task!!)
            2 -> handleWrongAnswerHint(task!!)
            3 -> handleWrongAnswerKick(task!!)
            4 -> handleTimeout(task!!)
        }
    }

    /** 验证成功：发送欢迎消息（任务已在锁内移除） */
    private suspend fun handleSuccess(task: AuthInfo) {
        val config = EGAFunction.readGroupAuthSetting(task.groupId)
        val event = task.event
        try {
            event.group.sendMessage(buildMessageChain {
                +event.member.at()
                if (config.welcomeSwitch) {
                    +PlainText("\n")
                    +config.welcomeMessage.deserializeMiraiCode()
                } else {
                    +PlainText(" 您已完成验证")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 答案错误但仍有剩余机会：发送错误提示（任务保留在列表中） */
    private suspend fun handleWrongAnswerHint(task: AuthInfo) {
        try {
            task.event.group.sendMessage(buildMessageChain {
                +task.event.member.at()
                +PlainText(" 验证码错误，你还有 ${task.chance} 次机会")
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 答案错误且机会耗尽：发送提示并踢出（任务已在锁内移除） */
    private suspend fun handleWrongAnswerKick(task: AuthInfo) {
        val event = task.event
        try {
            event.group.sendMessage(buildMessageChain {
                +event.member.at()
                +PlainText(" 验证码全部错误，请重试")
            })
            event.member.kick("请重试：验证码错误")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** 验证超时：发送提示并踢出（任务已在锁内移除） */
    private suspend fun handleTimeout(task: AuthInfo) {
        val event = task.event
        try {
            event.group.sendMessage(buildMessageChain {
                +event.member.at()
                +PlainText(" 验证码超时，请重试")
            })
            event.member.kick("请重试：验证码验证超时")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ======================== 任务管理 ========================

    /**
     * 移除指定群指定成员的验证任务。
     */
    private fun removeAuthTask(groupId: Long, userId: Long) {
        authTasks.removeAll { it.groupId == groupId && it.userId == userId }
    }

    /**
     * 添加一条新的验证任务。由进群监听器在生成验证码后调用。
     */
    suspend fun add(info: AuthInfo) {
        mutex.withLock {
            authTasks.add(info)
        }
    }

    /**
     * 取消指定群的所有验证任务。当群验证功能被关闭时调用。
     *
     * @param groupId 群号
     */
    suspend fun cancel(groupId: Long) {
        mutex.withLock {
            authTasks.removeAll { it.groupId == groupId }
        }
    }
}
