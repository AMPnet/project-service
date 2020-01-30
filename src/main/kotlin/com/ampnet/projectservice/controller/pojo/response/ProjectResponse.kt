package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.persistence.model.Project
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectResponse(
    val uuid: UUID,
    val name: String,
    val description: String,
    val location: String,
    val locationText: String,
    val returnOnInvestment: String,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val mainImage: String?,
    val active: Boolean,
    val tags: List<String>
) {
    constructor(project: Project) : this(
        project.uuid,
        project.name,
        project.description,
        project.location,
        project.locationText,
        project.returnOnInvestment,
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.mainImage,
        project.active,
        project.tags.orEmpty()
    )
}

data class ProjectListResponse(val projects: List<ProjectResponse>, val page: Int = 0, val totalPages: Int = 1)

data class ProjectFullResponse(
    val uuid: UUID,
    val name: String,
    val description: String,
    val location: String,
    val locationText: String,
    val returnOnInvestment: String,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val mainImage: String?,
    val active: Boolean,
    val tags: List<String>,
    val gallery: List<String>,
    val news: List<String>,
    val documents: List<DocumentResponse>
) {
    constructor(project: Project) : this(
        project.uuid,
        project.name,
        project.description,
        project.location,
        project.locationText,
        project.returnOnInvestment,
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.mainImage,
        project.active,
        project.tags.orEmpty(),
        project.gallery.orEmpty(),
        project.newsLinks.orEmpty(),
        project.documents.orEmpty().map { DocumentResponse(it) }
    )
}
