package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.response.ProjectLocationResponse
import com.ampnet.projectservice.controller.pojo.response.ProjectRoiResponse
import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.persistence.model.Project
import java.time.LocalDate
import java.util.UUID

data class ProjectServiceResponse(
    val uuid: UUID,
    val name: String,
    val description: String,
    val location: ProjectLocationResponse,
    val roi: ProjectRoiResponse,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val mainImage: String?,
    val active: Boolean,
    val tags: List<String>,
    val coop: String
) {
    constructor(project: Project) : this(
        project.uuid,
        project.name,
        project.description,
        ProjectLocationResponse(project.location),
        ProjectRoiResponse(project.roi),
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.mainImage,
        project.active,
        project.tags.orEmpty(),
        project.coop
    )
}
