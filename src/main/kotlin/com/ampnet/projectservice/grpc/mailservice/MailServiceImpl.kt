package com.ampnet.projectservice.grpc.mailservice

import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.OrganizationInvitationRequest
import com.ampnet.projectservice.config.ApplicationProperties
import io.grpc.StatusRuntimeException
import io.grpc.stub.StreamObserver
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class MailServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory,
    private val applicationProperties: ApplicationProperties
) : MailService {

    companion object : KLogging()

    private val mailServiceStub: MailServiceGrpc.MailServiceStub by lazy {
        val channel = grpcChannelFactory.createChannel("mail-service")
        MailServiceGrpc.newStub(channel)
    }

    override fun sendOrganizationInvitationMail(email: String, organizationName: String) {
        logger.debug { "Sending organization invitation mail" }
        try {
            val request = OrganizationInvitationRequest.newBuilder()
                .setEmail(email)
                .setOrganization(organizationName)
                .build()

            serviceWithTimeout()
                .sendOrganizationInvitation(request, createSteamObserver("organization invitation mail to: $email"))
        } catch (ex: StatusRuntimeException) {
            logger.warn("Failed to send deposit request mail.", ex)
        }
    }

    private fun serviceWithTimeout() = mailServiceStub
        .withDeadlineAfter(applicationProperties.grpc.mailServiceTimeout, TimeUnit.MILLISECONDS)

    private fun createSteamObserver(message: String) =
        object : StreamObserver<Empty> {
            override fun onNext(value: Empty?) {
                logger.info { "Successfully sent $message" }
            }

            override fun onError(t: Throwable?) {
                logger.warn { "Failed to sent $message. ${t?.message}" }
            }

            override fun onCompleted() {
                // successfully sent
            }
        }
}
