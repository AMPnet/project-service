package com.ampnet.projectservice.grpc.walletservice

import com.ampnet.walletservice.proto.WalletResponse
import java.util.UUID

data class WalletServiceResponse(
    val uuid: UUID,
    val owner: UUID,
    val activationData: String,
    val type: String,
    val currency: String,
    val hash: String
) {
    constructor(walletResponse: WalletResponse) : this(
        UUID.fromString(walletResponse.uuid),
        UUID.fromString(walletResponse.owner),
        walletResponse.activationData,
        walletResponse.type.name,
        walletResponse.currency,
        walletResponse.hash
    )
}
