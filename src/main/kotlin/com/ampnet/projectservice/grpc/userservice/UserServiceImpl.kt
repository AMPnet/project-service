package com.ampnet.projectservice.grpc.userservice

import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.GrpcException
import com.ampnet.userservice.proto.GetUsersByEmailRequest
import com.ampnet.userservice.proto.GetUsersRequest
import com.ampnet.userservice.proto.UserResponse
import com.ampnet.userservice.proto.UserServiceGrpc
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class UserServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : UserService {

    companion object : KLogging()

    private val serviceBlockingStub: UserServiceGrpc.UserServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("user-service")
        UserServiceGrpc.newBlockingStub(channel)
    }

    override fun getUsers(uuids: Iterable<UUID>): List<UserResponse> {
        logger.debug { "Fetching users: $uuids" }
        try {
            val request = GetUsersRequest.newBuilder()
                .addAllUuids(uuids.map { it.toString() })
                .build()
            val response = serviceWithTimeout()
                .getUsers(request).usersList
            logger.debug { "Fetched users: $response" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_USER, "Failed to fetch users. ${ex.localizedMessage}")
        }
    }

    override fun getUsersByEmail(emails: List<String>): List<UserResponse> {
        logger.debug { "Fetching users by emails: ${emails.joinToString()}" }
        try {
            val request = GetUsersByEmailRequest.newBuilder()
                .addAllEmails(emails)
                .build()
            val response = serviceWithTimeout()
                .getUsersByEmail(request).usersList
            logger.debug { "Fetched users by emails: $response" }
            return response
        } catch (ex: StatusRuntimeException) {
            throw GrpcException(ErrorCode.INT_GRPC_USER, "Failed to fetch users by emails. ${ex.localizedMessage}")
        }
    }

    private fun serviceWithTimeout() = serviceBlockingStub
        .withDeadlineAfter(applicationProperties.grpc.userServiceTimeout, TimeUnit.MILLISECONDS)
}
