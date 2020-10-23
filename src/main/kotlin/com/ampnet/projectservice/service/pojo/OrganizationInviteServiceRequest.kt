package com.ampnet.projectservice.service.pojo

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.controller.pojo.request.OrganizationInviteRequest
import java.util.UUID

data class OrganizationInviteServiceRequest(
    val emails: List<String>,
    val organizationUuid: UUID,
    val invitedByUser: UserPrincipal
) {
    constructor(request: OrganizationInviteRequest, organizationUuid: UUID, user: UserPrincipal) : this(
        request.emails, organizationUuid, user
    )
}
