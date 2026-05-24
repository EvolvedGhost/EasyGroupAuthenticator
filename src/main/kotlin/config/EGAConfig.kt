package com.evolvedghost.config

import com.evolvedghost.data.GroupAuthSetting
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

/**
 * 插件全局配置文件。
 *
 * 配置文件名固定为 `GroupConfigs.yml`，由 Mirai Console 自动加载和保存。
 * 包含全局超时检测周期和各群的独立验证配置。
 */
object EGAConfig : AutoSavePluginConfig("GroupConfigs") {

    /**
     * 超时检测周期（秒）。
     *
     * 插件会以此间隔定期扫描所有进行中的验证，踢出超时未完成的成员。
     * 值越大性能越好，但超时踢出的延迟也越大。
     */
    @ValueDescription("超时检查时间（秒），较长的时间有利于改善插件性能")
    val timeoutDetectCycle by value(10L)

    /**
     * 各群的验证配置映射表。
     *
     * Key 为群号，Value 为该群的 [GroupAuthSetting] 配置。
     * 未显式配置的群会在首次访问时自动生成默认配置。
     */
    @ValueDescription("此处为各群的配置项，12345为一个示例项")
    val groupSettings: MutableMap<Long, GroupAuthSetting> by value(mutableMapOf(12345L to GroupAuthSetting()))
}