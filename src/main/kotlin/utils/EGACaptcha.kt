package com.evolvedghost.utils

import com.pig4cloud.captcha.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * 验证码生成工具。
 *
 * 根据群配置的 [authMethod][com.evolvedghost.data.GroupAuthSetting.authMethod] 生成不同类型的验证码图片。
 */
object EGACaptcha {

    /**
     * 验证码生成结果。
     *
     * @property image  验证码图片的字节流，供上传至 QQ 群
     * @property answer 验证码的正确答案
     * @property format 图片格式（"png" 或 "gif"）
     */
    data class Captcha(val image: ByteArrayInputStream, val answer: String, val format: String)

    /** 英文静态验证码（默认） */
    private fun simpleCaptcha(): Captcha {
        val captcha = SpecCaptcha(130, 48)
        return captcha.toResult("png")
    }

    /** 英文动态 GIF 验证码 */
    private fun simpleGifCaptcha(): Captcha {
        val captcha = GifCaptcha(130, 48)
        return captcha.toResult("gif")
    }

    /** 中文静态验证码 */
    private fun chineseCaptcha(): Captcha {
        val captcha = ChineseCaptcha(130, 48)
        return captcha.toResult("png")
    }

    /** 中文动态 GIF 验证码 */
    private fun chineseGifCaptcha(): Captcha {
        val captcha = ChineseGifCaptcha(130, 48)
        return captcha.toResult("gif")
    }

    /**
     * 算术验证码。
     * @param hard 是否启用困难模式（更多位数、更大数值、支持更多运算符）
     */
    private fun arithmeticCaptcha(hard: Boolean): Captcha {
        val captcha = ArithmeticCaptcha(150, 48)
        if (hard) {
            captcha.len = 4
            captcha.supportAlgorithmSign(5)
            captcha.setDifficulty(100)
        }
        return captcha.toResult("png")
    }

    /**
     * 根据群配置获取对应类型的验证码。
     *
     * @param groupId 群号，用于读取该群的验证码类型配置
     * @return 生成的验证码结果
     */
    fun getCaptcha(groupId: Long): Captcha {
        val config = EGAFunction.readGroupAuthSetting(groupId)
        return when (config.authMethod) {
            2 -> simpleGifCaptcha()
            3 -> chineseCaptcha()
            4 -> chineseGifCaptcha()
            5 -> arithmeticCaptcha(hard = false)
            6 -> arithmeticCaptcha(hard = true)
            else -> simpleCaptcha()
        }
    }

    /**
     * 将各验证码对象输出为字节数组并封装为 [Captcha] 结果。
     * 利用扩展函数统一 ByteArrayOutputStream 转换逻辑，避免各方法重复代码。
     *
     * 注：由于 pig4cloud 各验证码类无公共基类接口，此处使用 Any 接收。
     */
    private fun Any.toResult(format: String): Captcha {
        val out = ByteArrayOutputStream()
        when (this) {
            is SpecCaptcha -> { this.out(out); return Captcha(ByteArrayInputStream(out.toByteArray()), this.text(), format) }
            is GifCaptcha -> { this.out(out); return Captcha(ByteArrayInputStream(out.toByteArray()), this.text(), format) }
            is ChineseCaptcha -> { this.out(out); return Captcha(ByteArrayInputStream(out.toByteArray()), this.text(), format) }
            is ChineseGifCaptcha -> { this.out(out); return Captcha(ByteArrayInputStream(out.toByteArray()), this.text(), format) }
            is ArithmeticCaptcha -> { this.out(out); return Captcha(ByteArrayInputStream(out.toByteArray()), this.text(), format) }
            else -> throw IllegalArgumentException("Unsupported captcha type: ${this::class}")
        }
    }
}
