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
    val gallery: List<String>,
    val news: List<String>,
    val active: Boolean
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
        project.gallery.orEmpty(),
        project.newsLinks.orEmpty(),
        project.active
    )
}

data class ProjectListResponse(val projects: List<ProjectResponse>)
