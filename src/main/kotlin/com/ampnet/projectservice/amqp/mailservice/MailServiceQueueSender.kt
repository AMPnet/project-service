package com.ampnet.projectservice.amqp.mailservice

import com.ampnet.projectservice.service.pojo.OrganizationInvitationMailRequest
import mu.KLogging
import org.springframework.amqp.AmqpException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MailServiceQueueSender(private val rabbitTemplate: RabbitTemplate) : MailService {

    companion object : KLogging()

    override fun sendOrganizationInvitationMail(request: OrganizationInvitationMailRequest) {
        val message = MailOrgInvitationMessage(request.emails, request.organizationName, request.sender, request.coop)
        logger.debug { "Sending mail confirmation: $message" }
        try {
            rabbitTemplate.convertAndSend(QUEUE_MAIL_ORG_INVITATION, message)
        } catch (ex: AmqpException) {
            logger.warn(ex) { "Failed to send AMQP message to queue: $QUEUE_MAIL_ORG_INVITATION" }
        }
    }

    data class MailOrgInvitationMessage(
        val emails: List<String>,
        val organizationName: String,
        val sender: UUID,
        val coop: String
    )
}

const val QUEUE_MAIL_ORG_INVITATION = "mail.project.org-invitation"
