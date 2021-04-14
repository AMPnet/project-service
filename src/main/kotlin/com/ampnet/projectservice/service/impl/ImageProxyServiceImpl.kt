package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.service.ImageProxyService
import com.ampnet.projectservice.service.pojo.ImageResponse
import org.springframework.stereotype.Service
import java.lang.Exception
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class ImageProxyServiceImpl(private val applicationProperties: ApplicationProperties) : ImageProxyService {

    private val key: ByteArray by lazy {
        hexStringToByteArray(applicationProperties.imageProxy.key)
    }
    private val salt: ByteArray by lazy {
        hexStringToByteArray(applicationProperties.imageProxy.salt)
    }

    override fun generateImageResponse(url: String?): ImageResponse? {
        if (url == null) return null
        val smallUrl = generateSignedUrlForImgProxy(url, ImageSize.SQUARE_SMALL)
        val mediumUrl = generateSignedUrlForImgProxy(url, ImageSize.WIDE_MEDIUM)
        val fullUrl = generateSignedUrlForImgProxy(url, ImageSize.FULL)
        return ImageResponse(smallUrl, mediumUrl, fullUrl, url)
    }

    @Throws(Exception::class)
    private fun generateSignedUrlForImgProxy(
        url: String,
        imageSize: ImageSize,
        resize: String = "auto",
        gravity: String = "ce"
    ): String {
        val algorithm = "HmacSHA256"
        val encodedUrl = Base64.getUrlEncoder().withoutPadding().encodeToString(url.toByteArray())
        val width = imageSize.width ?: ""
        val height = imageSize.height ?: ""
        val path = "/rs:$resize:$width:$height:0/g:$gravity/$encodedUrl${imageSize.extension}"
        val sha256HMAC: Mac = Mac.getInstance(algorithm).apply {
            init(SecretKeySpec(key, algorithm))
            update(salt)
        }
        val hash: String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(sha256HMAC.doFinal(path.toByteArray()))
        return "${applicationProperties.imageProxy.url}/$hash$path"
    }

    @Suppress("MagicNumber")
    private fun hexStringToByteArray(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Even-length string required" }
        val res = ByteArray(hex.length / 2)
        for (i in res.indices) {
            res[i] = (Character.digit(hex[i * 2], 16) shl 4 or Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
        return res
    }

    @Suppress("MagicNumber")
    enum class ImageSize(val width: Int?, val height: Int?, val extension: String? = null) {
        SQUARE_SMALL(140, 140),
        WIDE_MEDIUM(600, 300),
        FULL(null, null, ".jpg")
    }
}
