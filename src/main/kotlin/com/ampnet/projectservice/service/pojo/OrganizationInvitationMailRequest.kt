package com.ampnet.projectservice.service.pojo

import java.util.UUID

data class OrganizationInvitationMailRequest(
    val emails: List<String>,
    val organizationName: String,
    val sender: UUID,
    val coop: String
)
