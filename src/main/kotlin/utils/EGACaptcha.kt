package com.evolvedghost.utils

import com.pig4cloud.captcha.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


object EGACaptcha {
    data class Captcha(val image: ByteArrayInputStream, val answer: String, val format: String)

    private fun simpleCaptcha(): Captcha {
        val captcha = SpecCaptcha(130, 48)
        val out = ByteArrayOutputStream()
        captcha.out(out)
        return Captcha(ByteArrayInputStream(out.toByteArray()), captcha.text(), "png")
    }

    private fun simpleGifCaptcha(): Captcha {
        val captcha = GifCaptcha(130, 48)
        val out = ByteArrayOutputStream()
        captcha.out(out)
        return Captcha(ByteArrayInputStream(out.toByteArray()), captcha.text(), "gif")
    }

    private fun chineseCaptcha(): Captcha {
        val captcha = ChineseCaptcha(130, 48)
        val out = ByteArrayOutputStream()
        captcha.out(out)
        return Captcha(ByteArrayInputStream(out.toByteArray()), captcha.text(), "png")
    }

    private fun chineseGifCaptcha(): Captcha {
        val captcha = ChineseGifCaptcha(130, 48)
        val out = ByteArrayOutputStream()
        captcha.out(out)
        return Captcha(ByteArrayInputStream(out.toByteArray()), captcha.text(), "gif")
    }

    private fun arithmeticCaptcha(): Captcha {
        val captcha = ArithmeticCaptcha(130, 48)
        val out = ByteArrayOutputStream()
        captcha.supportAlgorithmSign(5)
        captcha.out(out)
        return Captcha(ByteArrayInputStream(out.toByteArray()), captcha.text(), "png")
    }

    fun getCaptcha(groupId: Long): Captcha {
        val config = EGAFunction.readGroupAuthSetting(groupId)
        return when (config.authMethod) {
            2 -> simpleGifCaptcha()
            3 -> chineseCaptcha()
            4 -> chineseGifCaptcha()
            5 -> arithmeticCaptcha()
            else -> simpleCaptcha()
        }
    }
}