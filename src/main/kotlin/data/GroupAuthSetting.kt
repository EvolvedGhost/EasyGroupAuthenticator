package com.evolvedghost.data

import kotlinx.serialization.Serializable

/**
 * 单个群的验证配置数据类。
 *
 * 每个群拥有独立的一份配置实例，由 [EGAConfig.groupSettings] 以群号为 key 持有。
 * 所有字段均有合理默认值，新建群配置时无需手动赋值。
 *
 * @property enable         是否启用本群的验证功能
 * @property authMethod     验证码类型：0=关闭, 1=英文静态, 2=英文动态(GIF), 3=中文静态, 4=中文动态(GIF), 5=简单算术, 6=困难算术
 * @property authChance     验证码允许的最大尝试次数，超过则踢出
 * @property authTime       验证码超时时间（秒），超时未验证则踢出
 * @property levelLimit     加群 QQ 等级下限，-1 表示不限制；低于此等级的成员进群后将被踢出
 * @property requestMethod  自动审核方式：0=关闭, 1=模糊匹配（消息包含关键词即可）, 2=精确匹配（消息完全等于关键词）
 * @property requestKeywords    自动审核通过关键词列表，申请消息命中任一关键词则自动通过
 * @property requestNegativeKeywords 自动审核拒绝关键词列表，申请消息命中任一关键词则自动拒绝
 * @property blackList      黑名单 QQ 号列表，名单内用户申请入群将被直接拒绝，已入群将被踢出
 * @property welcomeSwitch  是否启用新人欢迎消息
 * @property welcomeMessage 新人欢迎消息内容，支持 MiraiCode 格式（如 at、表情等）
 * @property memberChange   是否启用群成员变动（进群/退群）提醒
 */
@Serializable
data class GroupAuthSetting(
    var enable: Boolean = false,
    var authMethod: Int = 0,
    var authChance: Int = 3,
    var authTime: Int = 300,
    var levelLimit: Int = -1,
    var requestMethod: Int = 0,
    var requestKeywords: MutableList<String> = mutableListOf(),
    var requestNegativeKeywords: MutableList<String> = mutableListOf(),
    var blackList: MutableList<Long> = mutableListOf(),
    var welcomeSwitch: Boolean = false,
    var welcomeMessage: String = "进群欢迎信息",
    var memberChange: Boolean = false
)
