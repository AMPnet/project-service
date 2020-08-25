package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface OrganizationInviteRepository : JpaRepository<OrganizationInvitation, Int> {
    fun findByOrganizationUuidAndEmail(organizationUuid: UUID, email: String): Optional<OrganizationInvitation>
    fun findByEmail(email: String): List<OrganizationInvitation>

    @Query(
        "SELECT organizationInvitation FROM OrganizationInvitation organizationInvitation " +
            "INNER JOIN FETCH organizationInvitation.organization organization " +
            "WHERE organization.uuid = ?1"
    )
    fun findAllByOrganizationUuid(organizationUuid: UUID): List<OrganizationInvitation>
}
