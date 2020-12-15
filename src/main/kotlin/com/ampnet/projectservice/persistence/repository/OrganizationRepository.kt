package com.ampnet.projectservice.persistence.repository

import com.ampnet.projectservice.persistence.model.Organization
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface OrganizationRepository : JpaRepository<Organization, UUID> {

    @Query("SELECT org FROM Organization org LEFT JOIN FETCH org.documents WHERE org.uuid = ?1")
    fun findByIdWithDocuments(organizationUuid: UUID): Optional<Organization>

    // Each Organization in the list will have only one membership because of inner join
    @Query("SELECT org FROM Organization org INNER JOIN FETCH org.memberships mem WHERE mem.userUuid = ?1")
    fun findAllOrganizationsForUserUuid(userUuid: UUID): List<Organization>

    fun findByNameAndCoop(name: String, coop: String): Optional<Organization>

    fun findByNameContainingIgnoreCaseAndCoopAndActive(
        name: String,
        coop: String,
        active: Boolean,
        pageable: Pageable
    ): Page<Organization>

    @Query("SELECT org FROM Organization org LEFT JOIN FETCH org.memberships WHERE org.uuid = :organizationUuid")
    fun findByIdWithMemberships(organizationUuid: UUID): Optional<Organization>

    fun findByActive(active: Boolean, pageable: Pageable): Page<Organization>
}
