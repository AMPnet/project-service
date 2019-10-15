package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface OrganizationInviteRepository : JpaRepository<OrganizationInvitation, Int> {
    fun findByOrganizationUuidAndEmail(organizationUuid: UUID, email: String): Optional<OrganizationInvitation>
    fun findByEmail(email: String): List<OrganizationInvitation>
}
