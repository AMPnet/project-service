package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.service.pojo.ProjectWithWallet
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectResponse(
    val uuid: UUID,
    val name: String,
    val description: String,
    val location: ProjectLocationResponse,
    val roi: ProjectRoiResponse,
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
        project.tags.orEmpty()
    )
}

data class ProjectLocationResponse(val lat: Double, val long: Double) {
    constructor(location: ProjectLocation) : this(location.lat, location.long)
}

data class ProjectRoiResponse(val from: Double, val to: Double) {
    constructor(roi: ProjectRoi) : this(roi.from, roi.to)
}

data class ProjectListResponse(val projects: List<ProjectResponse>, val page: Int = 0, val totalPages: Int = 1)

data class ProjectFullResponse(
    val uuid: UUID,
    val name: String,
    val description: String,
    val location: ProjectLocationResponse,
    val roi: ProjectRoiResponse,
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
        project.gallery.orEmpty(),
        project.newsLinks.orEmpty(),
        project.documents.orEmpty().map { DocumentResponse(it) }
    )
}

data class ProjectWithWalletsResponse(
    val projectUuid: UUID,
    val projectName: String,
    val projectDescription: String,
    val projectLocation: ProjectLocationResponse,
    val projectRoi: ProjectRoiResponse,
    val projectStartDate: ZonedDateTime,
    val projectEndDate: ZonedDateTime,
    val projectExpectedFunding: Long,
    val projectCurrency: Currency,
    val projectMinPerUser: Long,
    val projectMaxPerUser: Long,
    val projectMainImage: String?,
    val projectActive: Boolean,
    val projectTags: List<String>,
    val walletUuid: UUID,
    val walletOwner: String,
    val walletActivationData: String,
    val walletType: String,
    val walletCurrency: String,
    val walletHash: String
) {
    constructor(projectWithWallet: ProjectWithWallet) : this(
        projectWithWallet.projectUuid,
        projectWithWallet.projectName,
        projectWithWallet.projectDescription,
        ProjectLocationResponse(projectWithWallet.projectLocation),
        ProjectRoiResponse(projectWithWallet.projectRoi),
        projectWithWallet.projectStartDate,
        projectWithWallet.projectEndDate,
        projectWithWallet.projectExpectedFunding,
        projectWithWallet.projectCurrency,
        projectWithWallet.projectMinPerUser,
        projectWithWallet.projectMaxPerUser,
        projectWithWallet.projectMainImage.orEmpty(),
        projectWithWallet.projectActive,
        projectWithWallet.projectTags.orEmpty(),
        projectWithWallet.walletUuid,
        projectWithWallet.walletOwner,
        projectWithWallet.walletActivationData,
        projectWithWallet.walletType,
        projectWithWallet.walletCurrency,
        projectWithWallet.walletHash
    )
}

data class ProjectWithWalletListResponse(
    val projectWithWallet: List<ProjectWithWalletsResponse>,
    val page: Int = 0,
    val totalPages: Int = 1
)
