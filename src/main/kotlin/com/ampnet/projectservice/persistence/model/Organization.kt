package com.ampnet.projectservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "organization")
data class Organization(
    @Id
    var uuid: UUID,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true)
    var legalInfo: String?,

    @Column(nullable = true)
    var createdByUserUuid: UUID,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = true)
    var updatedAt: ZonedDateTime?,

    @Column(nullable = false)
    var approved: Boolean,

    @Column
    var approvedByUserUuid: UUID?,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "organization_document",
            joinColumns = [JoinColumn(name = "organization_uuid")],
            inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var documents: List<Document>?,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationUuid")
    var memberships: List<OrganizationMembership>?
)
