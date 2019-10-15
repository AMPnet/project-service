package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationFollower
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationFollowerRepository : JpaRepository<OrganizationFollower, Int> {
    fun findByOrganizationUuid(organizationUuid: UUID): List<OrganizationFollower>
    fun findByUserUuidAndOrganizationUuid(userUuid: UUID, organizationUuid: UUID): Optional<OrganizationFollower>
}
