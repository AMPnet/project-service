package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.persistence.model.OrganizationInvitation

data class OrganizationInvitationWithName(
    val organizationInvitation: OrganizationInvitation,
    val organizationName: String
)
