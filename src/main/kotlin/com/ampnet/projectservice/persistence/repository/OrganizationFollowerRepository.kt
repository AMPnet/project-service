package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationFollower
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationFollowerRepository : JpaRepository<OrganizationFollower, Int> {
    fun findByOrganizationUuid(organizationUuid: UUID): List<OrganizationFollower>
    fun findByUserUuidAndOrganizationUuid(userUuid: UUID, organizationUuid: UUID): Optional<OrganizationFollower>
}
