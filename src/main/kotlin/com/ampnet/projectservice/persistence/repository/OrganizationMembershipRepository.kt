package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationMembership
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationMembershipRepository : JpaRepository<OrganizationMembership, Int> {
    fun findByOrganizationUuid(organizationUuid: UUID): List<OrganizationMembership>
    fun findByUserUuid(userUuid: UUID): List<OrganizationMembership>
    fun findByOrganizationUuidAndUserUuid(organizationUuid: UUID, userUuid: UUID): Optional<OrganizationMembership>
}
