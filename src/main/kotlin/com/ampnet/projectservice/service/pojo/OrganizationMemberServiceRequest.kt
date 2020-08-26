package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.UpdateOrganizationRoleRequest
import com.ampnet.projectservice.enums.OrganizationRoleType
import java.util.UUID

data class OrganizationMemberServiceRequest(
    val memberUuid: UUID,
    val organizationUuid: UUID,
    val roleType: OrganizationRoleType
) {
    constructor(organizationUuid: UUID, request: UpdateOrganizationRoleRequest) : this(
        request.memberUuid,
        organizationUuid,
        request.roleType
    )
}
