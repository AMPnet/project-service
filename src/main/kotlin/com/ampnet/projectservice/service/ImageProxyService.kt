package com.ampnet.projectservice.service

import com.ampnet.projectservice.service.pojo.ImageResponse

interface ImageProxyService {
    fun generateImageResponse(url: String?): ImageResponse?
}
