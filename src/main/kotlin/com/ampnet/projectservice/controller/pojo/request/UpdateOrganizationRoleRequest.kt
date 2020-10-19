package com.ampnet.projectservice.controller.pojo.request

import com.ampnet.projectservice.enums.OrganizationRole
import java.util.UUID

data class UpdateOrganizationRoleRequest(
    val memberUuid: UUID,
    val role: OrganizationRole
)
