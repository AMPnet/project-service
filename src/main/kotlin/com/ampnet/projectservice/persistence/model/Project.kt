package com.ampnet.projectservice.persistence.model

import com.ampnet.projectservice.enums.Currency
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.CollectionTable
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.Table
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode

@Entity
@Table(name = "project")
data class Project(
    @Id
    var uuid: UUID,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_uuid")
    var organization: Organization,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var description: String,

    @Column(nullable = false)
    var location: String,

    @Column(nullable = false)
    var locationText: String,

    @Column(nullable = false, length = 16)
    var returnOnInvestment: String,

    @Column(nullable = false)
    var startDate: ZonedDateTime,

    @Column(nullable = false)
    var endDate: ZonedDateTime,

    @Column(nullable = false)
    var expectedFunding: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    var currency: Currency,

    @Column(nullable = false)
    var minPerUser: Long,

    @Column(nullable = false)
    var maxPerUser: Long,

    @Column
    var mainImage: String?,

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @CollectionTable(name = "project_gallery", joinColumns = [JoinColumn(name = "project_uuid")])
    @Column(name = "image")
    var gallery: List<String>?,

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @CollectionTable(name = "project_news", joinColumns = [JoinColumn(name = "project_uuid")])
    @Column(name = "link")
    var newsLinks: List<String>?,

    @Column(nullable = true)
    var createdByUserUuid: UUID,

    @Column(nullable = false)
    var createdAt: ZonedDateTime,

    @Column(nullable = false)
    var active: Boolean,

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "project_document",
            joinColumns = [JoinColumn(name = "project_uuid")],
            inverseJoinColumns = [JoinColumn(name = "document_id")]
    )
    var documents: List<Document>?,

    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(value = FetchMode.SUBSELECT)
    @CollectionTable(name = "project_tag", joinColumns = [JoinColumn(name = "project_uuid")])
    @Column(name = "tag")
    var tags: List<String>?
)
