package com.ampnet.projectservice.controller.pojo.request

import com.ampnet.projectservice.enums.Currency
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectRequest(
    val organizationUuid: UUID,
    val name: String,
    val description: String,
    val location: ProjectLocationRequest,
    val roi: ProjectRoiRequest,
    val startDate: ZonedDateTime,
    val endDate: ZonedDateTime,
    val expectedFunding: Long,
    val currency: Currency,
    val minPerUser: Long,
    val maxPerUser: Long,
    val active: Boolean,
    val tags: List<String>? = null
)
data class ProjectLocationRequest(val lat: Double, val long: Double)
data class ProjectRoiRequest(val from: Double, val to: Double)
data class ProjectUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val location: ProjectLocationRequest? = null,
    val roi: ProjectRoiRequest? = null,
    val active: Boolean? = null,
    val tags: List<String>? = null,
    val news: List<String>? = null
)
