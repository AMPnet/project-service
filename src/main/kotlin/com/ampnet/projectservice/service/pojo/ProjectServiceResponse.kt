package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.response.ProjectLocationResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectRoiResponse
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.persistence.model.Project
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectServiceResponse(
    val uuid: UUID,
    val name: String,
    val description: String?,
    val location: ProjectLocationResponse,
    val roi: ProjectRoiResponse,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val mainImage: ImageResponse?,
    val active: Boolean,
    val tags: List<String>,
    val coop: String,
    val shortDescription: String?,
    val organization: OrganizationSmallServiceResponse,
    val ownerUuid: UUID
) {
    constructor(project: Project, mainImage: ImageResponse?, withDescription: Boolean = false) : this(
        project.uuid,
        project.name,
        if (withDescription) project.description else null,
        ProjectLocationResponse(project.location),
        ProjectRoiResponse(project.roi),
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        mainImage,
        project.active,
        project.tags.orEmpty(),
        project.coop,
        project.shortDescription,
        OrganizationSmallServiceResponse(project.organization),
        project.createdByUserUuid
    )
}
