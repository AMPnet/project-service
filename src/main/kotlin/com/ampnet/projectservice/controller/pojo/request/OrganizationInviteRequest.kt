package com.ampnet.projectservice.controller.pojo.request

import com.ampnet.projectservice.enums.OrganizationRoleType
import javax.validation.constraints.Email

data class OrganizationInviteRequest(
    @field:Email
    val email: String,
    val roleType: OrganizationRoleType
)
