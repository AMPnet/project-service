package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.userservice.proto.UserResponse
import java.time.ZonedDateTime
import java.util.UUID

data class OrganizationMembershipsResponse(val members: List<OrganizationMembershipResponse>)

data class OrganizationMembershipResponse(
    val uuid: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String,
    val memberSince: ZonedDateTime
) {
    constructor(membership: OrganizationMembership, userResponse: UserResponse?) : this(
        membership.userUuid,
        userResponse?.firstName.orEmpty(),
        userResponse?.lastName.orEmpty(),
        userResponse?.email.orEmpty(),
        membership.role.name,
        membership.createdAt
    )
}
