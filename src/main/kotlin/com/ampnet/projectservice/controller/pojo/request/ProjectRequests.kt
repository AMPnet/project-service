package com.ampnet.projectservice.controller.pojo.request

import com.ampnet.projectservice.enums.Currency
import java.time.LocalDate
import java.util.UUID
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

data class ProjectRequest(
    val organizationUuid: UUID,
    @field:Size(max = 256)
    val name: String,
    val description: String,
    val location: ProjectLocationRequest,
    val roi: ProjectRoiRequest,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @field:Positive
    val expectedFunding: Long,
    val currency: Currency,
    @field:Positive
    val minPerUser: Long,
    @field:Positive
    val maxPerUser: Long,
    val active: Boolean,
    val tags: List<String>? = null
)

data class ProjectLocationRequest(val lat: Double, val long: Double)
data class ProjectRoiRequest(val from: Double, val to: Double)
data class ProjectUpdateRequest(
    @field:Size(max = 256)
    val name: String? = null,
    val description: String? = null,
    val location: ProjectLocationRequest? = null,
    val roi: ProjectRoiRequest? = null,
    val active: Boolean? = null,
    val tags: List<String>? = null,
    val news: List<String>? = null
)
