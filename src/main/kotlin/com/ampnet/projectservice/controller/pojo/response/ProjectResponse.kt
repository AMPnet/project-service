package com.ampnet.projectservice.controller.pojo.response

import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.projectservice.service.pojo.ImageResponse
import com.ampnet.projectservice.service.pojo.OrganizationSmallServiceResponse
import com.ampnet.projectservice.service.pojo.ProjectServiceResponse
import com.ampnet.projectservice.service.pojo.ProjectWithWallet
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectLocationResponse(val lat: Double?, val long: Double?) {
    constructor(location: ProjectLocation) : this(location.lat, location.long)
}

data class ProjectRoiResponse(val from: Double?, val to: Double?) {
    constructor(roi: ProjectRoi) : this(roi.from, roi.to)
}

data class ProjectListResponse(val projects: List<ProjectServiceResponse>, val page: Int = 0, val totalPages: Int = 1)

data class ProjectWithWalletFullResponse(
    val uuid: UUID,
    val name: String,
    val description: String?,
    val location: ProjectLocationResponse?,
    val roi: ProjectRoiResponse?,
    val startDate: ZonedDateTime?,
    val endDate: ZonedDateTime?,
    val expectedFunding: Long?,
    val currency: Currency?,
    val minPerUser: Long?,
    val maxPerUser: Long?,
    val mainImage: String?,
    val image: ImageResponse?,
    val active: Boolean?,
    val tags: List<String>,
    val gallery: List<ImageResponse>,
    val news: List<String>,
    val documents: List<DocumentResponse>,
    val wallet: WalletServiceResponse?,
    val coop: String,
    val shortDescription: String?,
    val organization: OrganizationSmallServiceResponse,
    val ownerUuid: UUID
) {
    constructor(
        project: Project,
        wallet: WalletServiceResponse?,
        mainImage: ImageResponse?,
        gallery: List<ImageResponse>?
    ) : this(
        project.uuid,
        project.name,
        project.description,
        project.location?.let { ProjectLocationResponse(it) },
        project.roi?.let { ProjectRoiResponse(it) },
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.mainImage,
        mainImage,
        project.active,
        project.tags.orEmpty(),
        gallery.orEmpty(),
        project.newsLinks.orEmpty(),
        project.documents.orEmpty().map { DocumentResponse(it) },
        wallet,
        project.coop,
        project.shortDescription,
        OrganizationSmallServiceResponse(project.organization),
        project.createdByUserUuid
    )
}

data class ProjectsWalletsListResponse(
    val projectsWallets: List<ProjectWithWallet>,
    val page: Int = 0,
    val totalPages: Int = 1
)
