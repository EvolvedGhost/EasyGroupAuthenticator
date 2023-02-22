package com.evolvedghost.config

import com.evolvedghost.data.GroupAuthSetting
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object EGAConfig : AutoSavePluginConfig("GroupConfigs") {
    @ValueDescription("超时检查时间（秒），较长的时间有利于改善插件性能")
    val timeoutDetectCycle by value(10L)

    @ValueDescription("此处为各群的配置项，12345为一个示例项")
    val groupSettings: MutableMap<Long, GroupAuthSetting> by value(mutableMapOf(12345L to GroupAuthSetting()))
}