package com.ampnet.projectservice.service.pojo

import java.util.UUID

data class OrganizationInviteAnswerRequest(
    val userUuid: UUID,
    val email: String,
    val join: Boolean,
    val organizationId: Int
)
