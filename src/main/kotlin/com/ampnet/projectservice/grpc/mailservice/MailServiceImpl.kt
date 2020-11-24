package com.ampnet.projectservice.grpc.mailservice

import com.ampnet.mailservice.proto.Empty
import com.ampnet.mailservice.proto.MailServiceGrpc
import com.ampnet.mailservice.proto.OrganizationInvitationRequest
import com.ampnet.projectservice.config.ApplicationProperties
import com.ampnet.projectservice.service.pojo.OrganizationInvitationMailRequest
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

    override fun sendOrganizationInvitationMail(serviceRequest: OrganizationInvitationMailRequest) {
        val emails = serviceRequest.emails
        logger.debug { "Sending organization invitation mail to ${emails.joinToString()}" }
        try {
            val request = OrganizationInvitationRequest.newBuilder()
                .addAllEmails(emails)
                .setOrganization(serviceRequest.organizationName)
                .setSenderEmail(serviceRequest.senderEmail)
                .setCoop(serviceRequest.coop)
                .build()

            serviceWithTimeout()
                .sendOrganizationInvitation(
                    request, createSteamObserver("organization invitation mail to: ${emails.joinToString()}")
                )
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
