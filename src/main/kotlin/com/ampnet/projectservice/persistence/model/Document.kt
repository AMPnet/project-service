package com.ampnet.projectservice.persistence.model

import com.ampnet.projectservice.service.pojo.DocumentSaveRequest
import java.time.ZonedDateTime
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "document")
@Suppress("LongParameterList")
class Document(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false)
    val link: String,

    @Column(nullable = false)
    val name: String,

    @Column(length = 16)
    val type: String,

    @Column(nullable = false)
    val size: Int,

    @Column(nullable = false)
    val createdByUserUuid: UUID,

    @Column(nullable = false)
    val createdAt: ZonedDateTime
) {
    constructor(link: String, name: String, type: String, size: Int, createdByUserUuid: UUID) : this(
        0, link, name, type.take(MAX_DOCUMENT_TYPE_NAME), size, createdByUserUuid, ZonedDateTime.now()
    )

    constructor(link: String, request: DocumentSaveRequest) : this(
        0,
        link,
        request.name,
        request.type.take(MAX_DOCUMENT_TYPE_NAME),
        request.size,
        request.userUuid,
        ZonedDateTime.now()
    )
}

const val MAX_DOCUMENT_TYPE_NAME = 16
