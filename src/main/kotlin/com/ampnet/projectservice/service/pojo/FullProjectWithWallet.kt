package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse
import com.ampnet.projectservice.persistence.model.Project

data class FullProjectWithWallet(
    val project: Project,
    val walletResponse: WalletServiceResponse?
)
