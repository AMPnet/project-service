package com.ampnet.projectservice.service

import com.ampnet.projectservice.service.impl.ImageProxyServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ImageProxyServiceTest : JpaServiceTestBase() {

    private val service: ImageProxyService by lazy { ImageProxyServiceImpl(applicationProperties) }

    @Test
    fun mustGenerateCorrectUrl() {
        verify("Service will generate correct url") {
            val url = "http://img.example.com/pretty/image.jpg"
            val response = service.generateImageResponse(url) ?: fail("Missing image response")
            assertThat(response.original).isEqualTo(url)
            assertThat(response.full).startsWith(applicationProperties.imageProxy.url)
            assertThat(response.squareSmall).startsWith(applicationProperties.imageProxy.url)
            assertThat(response.wideMedium).startsWith(applicationProperties.imageProxy.url)
            assertThat(response.squareSmall).isNotEqualTo(response.wideMedium)
        }
    }
}
