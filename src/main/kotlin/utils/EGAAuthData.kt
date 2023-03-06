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
import net.mamoe.mirai.message.data.content

val authData = EGAAuthData()

val groupMessageListener = GlobalEventChannel.subscribeAlways<GroupMessageEvent> { event ->
    try {
        if (authData.hasData(event.group.id, event.sender.id)) {
            authData.verifyData(
                event.group.id,
                event.sender.id,
                event.message.content
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

}

@OptIn(DelicateCoroutinesApi::class)
class EGAAuthData {
    init {
        GlobalScope.launch {
            while (true) {
                delay(EGAConfig.timeoutDetectCycle * 1000)
                val timestamp = System.currentTimeMillis()
                authMutex.withLock {
                    val iterator = authData.iterator()
                    while (iterator.hasNext()) {
                        val authInfo = iterator.next()
                        if (timestamp > authInfo.timeout) {
                            try {
                                verifyFailure(authInfo.event)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }

    data class AuthInfo(
        val event: MemberJoinEvent,
        val botId: Long,
        val groupId: Long,
        val userId: Long,
        val timeout: Long,
        val answer: String,
        var chance: Int,
    )

    private val authData = mutableListOf<AuthInfo>()

    private val authMutex = Mutex()

    fun hasData(groupId: Long, userId: Long): Boolean {
        val iterator = authData.iterator()
        while (iterator.hasNext()) {
            val authInfo = iterator.next()
            if (authInfo.groupId == groupId && authInfo.userId == userId) {
                return true
            }
        }
        return false
    }

    private fun getData(groupId: Long, userId: Long): AuthInfo? {
        val iterator = authData.iterator()
        while (iterator.hasNext()) {
            val authInfo = iterator.next()
            if (authInfo.groupId == groupId && authInfo.userId == userId) {
                return authInfo
            }
        }
        return null
    }

    suspend fun verifyData(groupId: Long, userId: Long, answer: String) {
        authMutex.withLock {
            val data = getData(groupId, userId)
            if (data == null) {
                removeData(groupId, userId)
            } else if (answer.contains(data.answer)) {
                verifySuccess(data.event)
            } else {
                data.chance--
                verifyFailure(data.event, data.chance)
            }
        }
    }

    private suspend fun verifySuccess(event: MemberJoinEvent) {
        removeData(event.groupId, event.member.id)
        val config = EGAFunction.readGroupAuthSetting(event.groupId)
        if (!config.welcomeSwitch) {
            event.group.sendMessage(buildMessageChain {
                event.member.at()
                +PlainText(" 您已完成验证")
            })
        } else {
            event.group.sendMessage(buildMessageChain {
                event.member.at()
                +PlainText("\n")
                +config.welcomeMessage.deserializeMiraiCode()
            })
        }
    }

    private suspend fun verifyFailure(event: MemberJoinEvent, chance: Int) {
        if (chance <= 0) {
            removeData(event.groupId, event.member.id)
            event.group.sendMessage(buildMessageChain {
                event.member.at()
                +PlainText(" 验证码全部错误，请重试")
            })
            event.member.kick("请重试：验证码错误")
        } else {
            event.group.sendMessage(buildMessageChain {
                event.member.at()
                +PlainText(" 验证码错误，你还有 $chance 次机会")
            })
        }
    }

    private suspend fun verifyFailure(event: MemberJoinEvent) {
        removeData(event.groupId, event.member.id)
        event.group.sendMessage(buildMessageChain {
            event.member.at()
            +PlainText(" 验证码超时，请重试")
        })
        event.member.kick("请重试：验证码验证超时")
    }

    private fun removeData(groupId: Long, userId: Long) {
        val iterator = authData.iterator()
        while (iterator.hasNext()) {
            val info = iterator.next()
            if (info.groupId == groupId && info.userId == userId) {
                iterator.remove()
            }
        }
    }

    suspend fun add(info: AuthInfo) {
        authMutex.withLock {
            authData.add(info)
        }
    }

    suspend fun cancel(groupId: Long) {
        authMutex.withLock {
            val iterator = authData.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (info.groupId == groupId) {
                    iterator.remove()
                }
            }
        }
    }
}