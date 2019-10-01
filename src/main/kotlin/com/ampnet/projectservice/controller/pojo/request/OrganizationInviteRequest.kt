package com.ampnet.projectservice.controller.pojo.request

import com.ampnet.projectservice.enums.OrganizationRoleType

data class OrganizationInviteRequest(
    val email: String,
    val roleType: OrganizationRoleType
)
