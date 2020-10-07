package com.ampnet.projectservice.config

import mu.KLogging
import org.springframework.context.ResourceLoaderAware
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.ProtocolResolver
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64

class DataProtocolResolver : ProtocolResolver, ResourceLoaderAware {

    companion object : KLogging()

    override fun resolve(location: String, resourceLoader: ResourceLoader): Resource? {
        if (!location.startsWith("data:")) return null
        val dataSeparatorIndex = location.indexOf(',')
        val header = location.substring(0, dataSeparatorIndex)
        val data = location.substring(dataSeparatorIndex + 1)
        val parse = parse(data, header.endsWith(";base64"))
        return ByteArrayResource(parse!!, header)
    }

    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        if (resourceLoader is DefaultResourceLoader) resourceLoader.addProtocolResolver(this)
        else logger.warn(
            "Unable to register the 'data:' resource protocol to this kind of ResourceLoader: " +
                "{} (not a DefaultResourceLoader)",
            resourceLoader
        )
    }

    private fun parse(data: String, base64: Boolean): ByteArray? {
        return if (base64) {
            Base64.getDecoder().decode(data)
        } else {
            URLDecoder.decode(data, UTF_8).toByteArray()
        }
    }
}
