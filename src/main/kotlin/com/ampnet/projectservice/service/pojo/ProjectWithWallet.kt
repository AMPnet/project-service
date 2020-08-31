package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.enums.Currency
import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.projectservice.persistence.model.ProjectLocation
import com.ampnet.projectservice.persistence.model.ProjectRoi
import com.ampnet.walletservice.proto.WalletResponse
import java.time.ZonedDateTime
import java.util.UUID

data class ProjectWithWallet(
    val projectUuid: UUID,
    val projectName: String,
    val projectDescription: String,
    val projectLocation: ProjectLocation,
    val projectRoi: ProjectRoi,
    val projectStartDate: ZonedDateTime,
    val projectEndDate: ZonedDateTime,
    val projectExpectedFunding: Long,
    val projectCurrency: Currency,
    val projectMinPerUser: Long,
    val projectMaxPerUser: Long,
    val projectMainImage: String?,
    val projectActive: Boolean,
    val projectTags: List<String>?,
    val walletUuid: UUID,
    val walletOwner: String,
    val walletActivationData: String,
    val walletType: String,
    val walletCurrency: String,
    val walletHash: String
) {
    constructor(project: Project, wallet: WalletResponse) : this(
        project.uuid,
        project.name,
        project.description,
        project.location,
        project.roi,
        project.startDate,
        project.endDate,
        project.expectedFunding,
        project.currency,
        project.minPerUser,
        project.maxPerUser,
        project.mainImage,
        project.active,
        project.tags,
        UUID.fromString(wallet.uuid),
        wallet.owner,
        wallet.activationData,
        wallet.type.name,
        wallet.currency,
        wallet.hash
    )
}
