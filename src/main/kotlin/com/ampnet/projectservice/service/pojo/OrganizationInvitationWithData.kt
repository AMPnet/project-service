package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import java.util.UUID

data class OrganizationInvitationWithData(
    val organizationUuid: UUID,
    val organizationName: String,
    val role: OrganizationRole,
    val organizationHeaderImage: String?
) {
    constructor(invite: OrganizationInvitation) : this(
        invite.organization.uuid,
        invite.organization.name,
        invite.role,
        invite.organization.headerImage
    )
}
