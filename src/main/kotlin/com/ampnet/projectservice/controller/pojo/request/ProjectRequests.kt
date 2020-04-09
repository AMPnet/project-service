package com.ampnet.projectservice.controller.pojo.request

import com.ampnet.projectservice.enums.Currency
import java.time.ZonedDateTime
import java.util.UUID
import javax.validation.constraints.Size

data class ProjectRequest(
    val organizationUuid: UUID,
    @Size(max = 255)
    val name: String,
    @Size(max = 255)
    val description: String,
    val location: ProjectLocationRequest,
    @Size(max = 16)
    val returnOnInvestment: String,
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
data class ProjectUpdateRequest(
    @Size(max = 255)
    val name: String? = null,
    val description: String? = null,
    val location: ProjectLocationRequest? = null,
    val returnOnInvestment: String? = null,
    val active: Boolean? = null,
    val tags: List<String>? = null,
    val news: List<String>? = null
)
