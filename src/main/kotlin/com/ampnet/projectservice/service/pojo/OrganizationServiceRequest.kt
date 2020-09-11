package com.ampnet.projectservice.service.pojo

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import org.springframework.web.multipart.MultipartFile

data class OrganizationServiceRequest(
    val name: String,
    val owner: UserPrincipal,
    val headerImage: MultipartFile,
    val description: String
) {
    constructor(request: OrganizationRequest, user: UserPrincipal, headerImage: MultipartFile) : this(
        request.name,
        user,
        headerImage,
        request.description
    )
}
