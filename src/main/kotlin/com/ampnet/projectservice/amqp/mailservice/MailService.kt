package com.ampnet.projectservice.amqp.mailservice

import com.ampnet.projectservice.service.pojo.OrganizationInvitationMailRequest

interface MailService {
    fun sendOrganizationInvitationMail(request: OrganizationInvitationMailRequest)
}
