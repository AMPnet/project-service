package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.grpc.walletservice.WalletServiceResponse

data class ProjectWithWallet(
    val project: ProjectServiceResponse,
    val wallet: WalletServiceResponse?
)
