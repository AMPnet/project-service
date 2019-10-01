package com.ampnet.projectservice.persistence.model

import com.ampnet.projectservice.enums.OrganizationPrivilegeType
import com.ampnet.projectservice.enums.OrganizationRoleType
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "organization_membership")
data class OrganizationMembership(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var organizationUuid: UUID,

    @Column(nullable = false)
    var userUuid: UUID,

    @ManyToOne
    @JoinColumn(name = "role_id")
    var role: Role,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
) {
    private fun getPrivileges(): List<OrganizationPrivilegeType> =
            OrganizationRoleType.fromInt(role.id)?.getPrivileges().orEmpty()

    fun hasPrivilegeToSeeOrganizationUsers(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PR_USERS)

    fun hasPrivilegeToWriteOrganizationUsers(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_USERS)

    fun hasPrivilegeToWriteOrganization(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_ORG)

    fun hasPrivilegeToWriteProject(): Boolean = getPrivileges().contains(OrganizationPrivilegeType.PW_PROJECT)
}
