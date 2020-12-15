package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.OrganizationUpdateRequest
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class OrganizationUpdateServiceRequest(
    val organizationUuid: UUID,
    val headerImage: MultipartFile?,
    val description: String?,
    val active: Boolean?
) {
    constructor(organizationUuid: UUID, headerImage: MultipartFile?, request: OrganizationUpdateRequest) : this(
        organizationUuid,
        headerImage,
        request.description,
        request.active
    )
}
