package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.UpdateOrganizationRoleRequest
import com.ampnet.projectservice.enums.OrganizationRole
import java.util.UUID

data class OrganizationMemberServiceRequest(
    val memberUuid: UUID,
    val organizationUuid: UUID,
    val roleType: OrganizationRole
) {
    constructor(organizationUuid: UUID, request: UpdateOrganizationRoleRequest) : this(
        request.memberUuid,
        organizationUuid,
        request.role
    )
}
