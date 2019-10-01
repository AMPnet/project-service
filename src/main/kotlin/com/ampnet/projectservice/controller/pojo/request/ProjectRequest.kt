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

    @Size(max = 128)
    val location: String,

    @Size(max = 255)
    val locationText: String,

    @Size(max = 16)
    val returnOnInvestment: String,

    val startDate: ZonedDateTime,

    val endDate: ZonedDateTime,

    val expectedFunding: Long,

    val currency: Currency,

    val minPerUser: Long,

    val maxPerUser: Long,

    val active: Boolean
)
