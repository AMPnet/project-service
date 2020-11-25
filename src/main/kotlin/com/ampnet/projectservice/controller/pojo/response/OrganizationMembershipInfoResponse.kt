package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.userservice.proto.UserResponse
import java.time.ZonedDateTime

data class OrganizationMembershipsInfoResponse(val members: List<OrganizationMembershipInfoResponse>)

data class OrganizationMembershipInfoResponse(
    val firstName: String,
    val lastName: String,
    val role: String,
    val memberSince: ZonedDateTime
) {
    constructor(membership: OrganizationMembership, userResponse: UserResponse?) : this(
        userResponse?.firstName.orEmpty(),
        userResponse?.lastName.orEmpty(),
        membership.role.name,
        membership.createdAt
    )
}
