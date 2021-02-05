package com.ampnet.projectservice.amqp.mailservice

import com.ampnet.projectservice.service.pojo.OrganizationInvitationMailRequest
import mu.KLogging
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service

@Service
class MailServiceQueueSender(private val rabbitTemplate: RabbitTemplate) : MailService {

    companion object : KLogging()

    override fun sendOrganizationInvitationMail(request: OrganizationInvitationMailRequest) {
        val message =
            MailOrgInvitationMessage(request.emails, request.organizationName, request.senderEmail, request.coop)
        logger.debug { "Sending mail confirmation: $message" }
        rabbitTemplate.convertAndSend(QUEUE_MAIL_ORG_INVITATION, message)
    }

    data class MailOrgInvitationMessage(
        val emails: List<String>,
        val organization: String,
        val senderEmail: String,
        val coop: String
    )
}

const val QUEUE_MAIL_ORG_INVITATION = "mail.project.org-invitation"
