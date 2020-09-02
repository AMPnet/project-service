package com.ampnet.projectservice.grpc.walletservice

import com.ampnet.walletservice.proto.WalletResponse
import java.util.UUID

interface WalletService {
    fun getWallets(uuids: List<UUID>): List<WalletResponse>
}
