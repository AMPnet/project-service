package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.controller.pojo.request.ProjectRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectRoiRequest
import com.ampnet.projectservice.controller.pojo.request.ProjectUpdateRequest
import java.time.ZonedDateTime

data class ProjectValidation(
    val startDate: ZonedDateTime? = null,
    val endDate: ZonedDateTime? = null,
    val minPerUser: Long? = null,
    val maxPerUser: Long? = null,
    val expectedFunding: Long? = null,
    val roi: ProjectRoiRequest? = null
) {
    constructor(request: ProjectRequest) : this(
        request.startDate,
        request.endDate,
        request.minPerUser,
        request.maxPerUser,
        request.expectedFunding,
        request.roi
    )
    constructor(request: ProjectUpdateRequest) : this(
        request.startDate,
        request.endDate,
        request.minPerUser,
        request.maxPerUser,
        request.expectedFunding,
        request.roi
    )
}
