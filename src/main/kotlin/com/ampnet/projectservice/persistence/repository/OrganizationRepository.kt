package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Organization
import java.util.Optional
import java.util.UUID
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OrganizationRepository : JpaRepository<Organization, UUID> {

    @Query("SELECT org FROM Organization org LEFT JOIN FETCH org.documents WHERE org.uuid = ?1")
    fun findByIdWithDocuments(organizationUuid: UUID): Optional<Organization>

    // Each Organization in the list will have only one membership because of inner join
    @Query("SELECT org FROM Organization org INNER JOIN FETCH org.memberships mem WHERE mem.userUuid = ?1")
    fun findAllOrganizationsForUserUuid(userUuid: UUID): List<Organization>

    fun findByName(name: String): Optional<Organization>

    fun findByNameContainingIgnoreCase(name: String, pageable: Pageable): Page<Organization>
}
