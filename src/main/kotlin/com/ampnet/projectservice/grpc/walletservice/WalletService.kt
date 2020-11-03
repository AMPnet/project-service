package com.ampnet.projectservice.grpc.walletservice

import java.util.UUID

interface WalletService {
    fun getWalletsByOwner(uuids: List<UUID>): List<WalletServiceResponse>
}
