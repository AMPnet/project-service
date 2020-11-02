package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.walletservice.proto.WalletResponse

data class ProjectWithWallet(
    val project: Project,
    val wallet: WalletResponse
)

data class ProjectWithWalletOptional(
    val project: ProjectServiceResponse,
    val wallet: WalletServiceResponse?
)
