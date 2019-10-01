package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.OrganizationRequest
import java.util.UUID

data class OrganizationServiceRequest(
    val name: String,
    val legalInfo: String,
    val ownerUuid: UUID
) {
    constructor(request: OrganizationRequest, userUuid: UUID) : this(
            request.name,
            request.legalInfo,
            userUuid
    )
}
