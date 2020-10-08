package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.service.pojo.OrganizationInvitationWithName
import java.util.UUID

data class OrganizationInviteResponse(
    val organizationUuid: UUID,
    val organizationName: String,
    val role: OrganizationRoleType?
) {
    constructor(invite: OrganizationInvitationWithName) : this(
        invite.organizationInvitation.organizationUuid,
        invite.organizationName,
        OrganizationRoleType.fromInt(invite.organizationInvitation.role.id)
    )
}

data class OrganizationInvitesListResponse(val organizationInvites: List<OrganizationInviteResponse>)
