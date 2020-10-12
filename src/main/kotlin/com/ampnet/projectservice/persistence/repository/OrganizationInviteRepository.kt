package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrganizationInviteRepository : JpaRepository<OrganizationInvitation, Int> {
    fun findByOrganizationUuidAndEmail(organizationUuid: UUID, email: String): Optional<OrganizationInvitation>
    fun findByEmail(email: String): List<OrganizationInvitation>
    fun findByOrganizationUuid(organizationUuid: UUID): List<OrganizationInvitation>
    fun findByOrganizationUuidAndEmailIn(organizationUuid: UUID, email: List<String>): List<OrganizationInvitation>
}
