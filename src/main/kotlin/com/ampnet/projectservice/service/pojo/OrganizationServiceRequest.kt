package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class OrganizationServiceRequest(
    val name: String,
    val ownerUuid: UUID,
    val headerImage: MultipartFile,
    val description: String
) {
    constructor(request: OrganizationRequest, userUuid: UUID, headerImage: MultipartFile) : this(
        request.name,
        userUuid,
        headerImage,
        request.description
    )
}
