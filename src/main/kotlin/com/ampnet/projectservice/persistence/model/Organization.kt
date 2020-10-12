package com.ampnet.projectservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "organization")
@Suppress("LongParameterList")
class Organization(
    @Id
    var uuid: UUID,

    @Column(nullable = false)
    var name: String,

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
    @JoinColumn(name = "organizationUuid")
    var documents: MutableList<Document>?,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizationUuid")
    var memberships: MutableList<OrganizationMembership>?,

    @Column
    var headerImage: String?,

    @Column
    var description: String?

) {
    constructor(name: String, createdByUserUuid: UUID, headerImage: String?, description: String?) : this(
        UUID.randomUUID(), name, createdByUserUuid, ZonedDateTime.now(),
        null, true, null, null,
        null, headerImage, description
    )
}
