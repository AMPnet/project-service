package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse
import com.ampnet.projectservice.persistence.model.Project

data class ProjectWithWallet(
    val project: ProjectServiceResponse,
    val wallet: WalletServiceResponse?
) {
    constructor(project: Project, wallet: WalletServiceResponse? = null) : this(
        ProjectServiceResponse(project), wallet
    )
}
