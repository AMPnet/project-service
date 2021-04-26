package com.ampnet.projectservice.persistence.model

import com.ampnet.projectservice.enums.Currency
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "project")
@Suppress("LongParameterList")
class Project(
    @Id
    var uuid: UUID,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_uuid", nullable = false)
    var organization: Organization,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = true)
    var description: String?,

    @Embedded
    var location: ProjectLocation?,

    @Embedded
    var roi: ProjectRoi?,

    @Column(nullable = true)
    var startDate: ZonedDateTime?,

    @Column(nullable = true)
    var endDate: ZonedDateTime?,

    @Column(nullable = true)
    var expectedFunding: Long?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 3)
    var currency: Currency?,

    @Column(nullable = true)
    var minPerUser: Long?,

    @Column(nullable = true)
    var maxPerUser: Long?,

    @Column
    var mainImage: String?,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_gallery", joinColumns = [JoinColumn(name = "project_uuid")])
    @Column(name = "image")
    var gallery: List<String>?,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "project_news", joinColumns = [JoinColumn(name = "project_uuid")])
    @Column(name = "link")
    var newsLinks: List<String>?,

    @Column(nullable = true)
    var createdByUserUuid: UUID,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = true)
    var active: Boolean?,

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_uuid")
    var documents: MutableList<Document>?,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_tag", joinColumns = [JoinColumn(name = "project_uuid")])
    @Column(name = "tag")
    var tags: List<String>?,

    @Column(nullable = false)
    var coop: String,

    @Column(nullable = true)
    var shortDescription: String?
)
