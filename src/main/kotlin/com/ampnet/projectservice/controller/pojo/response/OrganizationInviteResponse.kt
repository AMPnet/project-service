package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.OrganizationInvitation

data class OrganizationInviteResponse(
    val organizationId: Int,
    val organizationName: String,
    val role: OrganizationRoleType?
) {
    constructor(invite: OrganizationInvitation) : this(
            invite.organizationId,
            invite.organization?.name ?: "Missing value",
            OrganizationRoleType.fromInt(invite.role.id)
    )
}

data class OrganizationInvitesListResponse(val organizationInvites: List<OrganizationInviteResponse>)
