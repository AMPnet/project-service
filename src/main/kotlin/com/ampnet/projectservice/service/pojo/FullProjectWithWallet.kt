package com.ampnet.projectservice.service.pojo

import com.ampnet.projectservice.persistence.model.Project
import com.ampnet.walletservice.proto.WalletResponse

data class FullProjectWithWallet(
    val project: Project,
    val walletResponse: WalletResponse?
)
