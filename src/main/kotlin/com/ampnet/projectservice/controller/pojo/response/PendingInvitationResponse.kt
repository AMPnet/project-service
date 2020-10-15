package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import java.time.ZonedDateTime

data class PendingInvitationResponse(
    val userEmail: String,
    val role: OrganizationRole,
    val createdAt: ZonedDateTime

) {
    constructor(invite: OrganizationInvitation) : this(
        invite.email,
        invite.role,
        invite.createdAt
    )
}

data class PendingInvitationsListResponse(val pendingInvites: List<PendingInvitationResponse>)
