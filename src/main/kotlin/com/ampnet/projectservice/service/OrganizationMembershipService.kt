package com.ampnet.projectservice.service

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.service.pojo.OrganizationMemberServiceRequest
import java.util.UUID

interface OrganizationMembershipService {
    fun addUserToOrganization(
        userUuid: UUID,
        organizationUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationMembership

    fun removeUserFromOrganization(userUuid: UUID, organizationUuid: UUID)
    fun getOrganizationMemberships(organizationUuid: UUID): List<OrganizationMembership>
    fun updateOrganizationRole(request: OrganizationMemberServiceRequest)
}
