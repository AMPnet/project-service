package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.OrganizationInviteRequest
import java.util.UUID

data class OrganizationInviteServiceRequest(
    val emails: List<String>,
    val organizationUuid: UUID,
    val invitedByUserUuid: UUID
) {
    constructor(request: OrganizationInviteRequest, organizationUuid: UUID, userUuid: UUID) : this(
        request.emails, organizationUuid, userUuid
    )
}
