package com.ampnet.projectservice.grpc.mailservice

import com.ampnet.projectservice.service.pojo.OrganizationInvitationMailRequest

interface MailService {
    fun sendOrganizationInvitationMail(serviceRequest: OrganizationInvitationMailRequest)
}
