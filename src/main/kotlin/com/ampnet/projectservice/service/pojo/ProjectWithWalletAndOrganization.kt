package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse

data class ProjectWithWalletAndOrganization(
    val project: ProjectServiceResponse,
    val wallet: WalletServiceResponse?,
    val organization: OrganizationServiceResponse
)
