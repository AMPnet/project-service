package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import java.time.ZonedDateTime

data class PendingInvitationResponse(
    val userEmail: String,
    val role: OrganizationRoleType?,
    val createdAt: ZonedDateTime

) {
    constructor(invite: OrganizationInvitation) : this(
        invite.email,
        OrganizationRoleType.fromInt(invite.role.id),
        invite.createdAt
    )
}

data class PendingInvitationsListResponse(val pendingInvites: List<PendingInvitationResponse>)
