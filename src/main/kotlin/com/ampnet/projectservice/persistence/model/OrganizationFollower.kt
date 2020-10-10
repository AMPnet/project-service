package com.ampnet.projectservice.persistence.model

import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "organization_follower")
class OrganizationFollower(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    var organizationUuid: UUID,

    @Column(nullable = false)
    var userUuid: UUID,

    @Column(nullable = false)
    var createdAt: ZonedDateTime
)
