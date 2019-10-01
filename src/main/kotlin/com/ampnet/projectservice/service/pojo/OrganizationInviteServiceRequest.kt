package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.OrganizationInviteRequest
import com.ampnet.projectservice.enums.OrganizationRoleType
import java.util.UUID

data class OrganizationInviteServiceRequest(
    val email: String,
    val roleType: OrganizationRoleType,
    val organizationUuid: UUID,
    val invitedByUserUuid: UUID
) {
    constructor(request: OrganizationInviteRequest, organizationUuid: UUID, userUuid: UUID) : this(
        request.email, request.roleType, organizationUuid, userUuid
    )
}
