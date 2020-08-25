package com.ampnet.projectservice.service

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import java.util.UUID

interface OrganizationMemberService {
    fun addUserToOrganization(
        userUuid: UUID,
        organizationUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationMembership

    fun removeUserFromOrganization(userUuid: UUID, organizationUuid: UUID)
    fun getOrganizationMemberships(organizationUuid: UUID): List<OrganizationMembership>
}
