package com.ampnet.projectservice.grpc.walletservice

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.GrpcException
import com.ampnet.walletservice.proto.GetWalletsByOwnerRequest
import com.ampnet.walletservice.proto.WalletServiceGrpc
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class WalletServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : WalletService {

    companion object : KLogging()

    private val walletServiceStub: WalletServiceGrpc.WalletServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("wallet-service")
        WalletServiceGrpc.newBlockingStub(channel)
    }

    override fun getWalletsByOwner(uuids: List<UUID>): List<WalletServiceResponse> {
        if (uuids.isEmpty()) return emptyList()
        logger.debug { "Fetching wallets for owners: $uuids" }
        try {
            val request = GetWalletsByOwnerRequest.newBuilder()
                .addAllOwnersUuids(uuids.map { it.toString() })
                .build()
            val response = serviceWithTimeout()
                .getWalletsByOwner(request).walletsList
            logger.debug { "Fetched wallets: ${response.size}" }
            return response.map { WalletServiceResponse(it) }
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_WALLET, "Failed to fetch wallets. ${ex.localizedMessage}")
        }
    }

    private fun serviceWithTimeout() = walletServiceStub
        .withDeadlineAfter(applicationProperties.grpc.walletServiceTimeout, TimeUnit.MILLISECONDS)
}
