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
        val smallUrl = generateSignedUrlForImgProxy(url, ImageSize.SMALL)
        val mediumUrl = generateSignedUrlForImgProxy(url, ImageSize.MEDIUM)
        return ImageResponse(smallUrl, mediumUrl, url)
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

    @Throws(Exception::class)
    fun generateSignedUrlForImgProxy(
        url: String,
        imageSize: ImageSize,
        resize: String = "fill",
        gravity: String = "sm",
        enlarge: Int = 0,
        // extension: String
    ): String {
        val algorithm = "HmacSHA256"
        // val encodedUrl: String = Base64.getUrlEncoder().withoutPadding().encodeToString(url.toByteArray())
        // val path = "/$resize/$width/$height/$gravity/$enlarge/$encodedUrl.$extension"
        val path = "/$resize/${imageSize.width}/${imageSize.height}/$gravity/$enlarge/plain/$url"
        val sha256HMAC: Mac = Mac.getInstance(algorithm).apply {
            init(SecretKeySpec(key, algorithm))
            update(salt)
        }
        val hash: String = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(sha256HMAC.doFinal(path.toByteArray()))
        return "${applicationProperties.imageProxy.url}/$hash$path"
    }

    @Suppress("MagicNumber")
    enum class ImageSize(val width: Int, val height: Int) {
        SMALL(140, 140), MEDIUM(600, 300)
    }
}
