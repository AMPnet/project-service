package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import java.util.UUID

data class OrganizationInvitationWithData(
    val organizationUuid: UUID,
    val organizationName: String,
    val role: OrganizationRoleType?
) {
    constructor(invite: OrganizationInvitation) : this(
        invite.organization.uuid,
        invite.organization.name,
        OrganizationRoleType.fromInt(invite.role.id)
    )
}
