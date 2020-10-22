package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface OrganizationInviteRepository : JpaRepository<OrganizationInvitation, Int> {
    fun findByOrganizationUuidAndEmail(organizationUuid: UUID, email: String): Optional<OrganizationInvitation>

    @Query(
        "SELECT invitation FROM OrganizationInvitation invitation " +
            "INNER JOIN FETCH invitation.organization " +
            "WHERE invitation.email = ?1 AND invitation.organization.coop = ?2"
    )
    fun findAllByEmailAndCoop(email: String, coop: String): List<OrganizationInvitation>

    fun findByOrganizationUuid(organizationUuid: UUID): List<OrganizationInvitation>
    fun findByOrganizationUuidAndEmailIn(organizationUuid: UUID, email: List<String>): List<OrganizationInvitation>
}
