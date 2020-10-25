package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.enums.OrganizationRole
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.grpc.mailservice.MailService
import com.ampnet.projectservice.grpc.userservice.UserService
import com.ampnet.projectservice.persistence.model.Organization
import com.ampnet.projectservice.persistence.model.OrganizationFollower
import com.ampnet.projectservice.persistence.model.OrganizationInvitation
import com.ampnet.projectservice.persistence.repository.OrganizationFollowerRepository
import com.ampnet.projectservice.persistence.repository.OrganizationInviteRepository
import com.ampnet.projectservice.service.OrganizationInviteService
import com.ampnet.projectservice.service.OrganizationMembershipService
import com.ampnet.projectservice.service.OrganizationService
import com.ampnet.projectservice.service.pojo.OrganizationInvitationWithData
import com.ampnet.projectservice.service.pojo.OrganizationInviteAnswerRequest
import com.ampnet.projectservice.service.pojo.OrganizationInviteServiceRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OrganizationInviteServiceImpl(
    private val inviteRepository: OrganizationInviteRepository,
    private val followerRepository: OrganizationFollowerRepository,
    private val mailService: MailService,
    private val organizationService: OrganizationService,
    private val organizationMembershipService: OrganizationMembershipService,
    private val userService: UserService
) : OrganizationInviteService {

    companion object : KLogging()

    @Transactional
    override fun sendInvitation(request: OrganizationInviteServiceRequest) {
        val invitedToOrganization = organizationService.findByIdWithMemberships(request.organizationUuid)
            ?: throw ResourceNotFoundException(
                ErrorCode.ORG_MISSING,
                "Missing organization with id: ${request.organizationUuid}"
            )
        throwExceptionForDuplicatedInvitations(request)
        throwExceptionForAlreadyInOrganization(invitedToOrganization, request.emails)

        val invites = request.emails.map { email ->
            OrganizationInvitation(
                0, email, request.invitedByUser.uuid,
                OrganizationRole.ORG_MEMBER, ZonedDateTime.now(), invitedToOrganization
            )
        }
        inviteRepository.saveAll(invites)
        mailService.sendOrganizationInvitationMail(
            request.emails, invitedToOrganization.name, request.invitedByUser.email
        )
        logger.debug { "Users: ${request.emails.joinToString()} invited to organization: ${request.organizationUuid}" }
    }

    @Transactional
    override fun revokeInvitation(organizationUuid: UUID, email: String) {
        inviteRepository.findByOrganizationUuidAndEmail(organizationUuid, email).ifPresent {
            logger.debug { "Revoked user: $email invitation to organization: $organizationUuid" }
            inviteRepository.delete(it)
        }
    }

    @Transactional(readOnly = true)
    override fun getAllInvitationsForUser(email: String): List<OrganizationInvitationWithData> {
        val invites = inviteRepository.findAllByEmail(email)
        return invites.map { OrganizationInvitationWithData(it) }
    }

    @Transactional
    override fun answerToInvitation(request: OrganizationInviteAnswerRequest) {
        inviteRepository.findByOrganizationUuidAndEmail(request.organizationUuid, request.email).ifPresent {
            if (request.join) {
                organizationMembershipService.addUserToOrganization(request.userUuid, it.organization.uuid, it.role)
            }
            inviteRepository.delete(it)
            logger.debug {
                "User: ${request.userUuid} answer = ${request.join} " +
                    "to join organization: ${request.organizationUuid}"
            }
        }
    }

    @Transactional
    override fun followOrganization(userUuid: UUID, organizationUuid: UUID): OrganizationFollower {
        ServiceUtils.wrapOptional(
            followerRepository.findByUserUuidAndOrganizationUuid(userUuid, organizationUuid)
        )?.let {
            return it
        }
        val follower = OrganizationFollower::class.java.getConstructor().newInstance()
        follower.userUuid = userUuid
        follower.organizationUuid = organizationUuid
        follower.createdAt = ZonedDateTime.now()
        return followerRepository.save(follower)
    }

    @Transactional
    override fun unfollowOrganization(userUuid: UUID, organizationUuid: UUID) {
        ServiceUtils.wrapOptional(
            followerRepository.findByUserUuidAndOrganizationUuid(userUuid, organizationUuid)
        )?.let {
            followerRepository.delete(it)
        }
    }

    @Transactional(readOnly = true)
    override fun getPendingInvitations(organizationUuid: UUID): List<OrganizationInvitation> {
        return inviteRepository.findByOrganizationUuid(organizationUuid)
    }

    private fun throwExceptionForDuplicatedInvitations(request: OrganizationInviteServiceRequest) {
        val existingInvitations =
            inviteRepository.findByOrganizationUuidAndEmailIn(request.organizationUuid, request.emails)
        if (existingInvitations.isNotEmpty()) {
            val emails = existingInvitations.joinToString { it.email }
            throw ResourceAlreadyExistsException(
                ErrorCode.ORG_DUPLICATE_INVITE,
                "Some users are already invited: $emails",
                mapOf("emails" to emails)
            )
        }
    }

    private fun throwExceptionForAlreadyInOrganization(organization: Organization, emails: List<String>) {
        val users = userService.getUsersByEmail(emails)
        if (users.isNotEmpty()) {
            val memberships = organization.memberships
            if (memberships?.isNotEmpty() == true) {
                val userMembershipMap = memberships.associateBy { it.userUuid.toString() }
                users.filter { user ->
                    userMembershipMap.containsKey(user.uuid)
                }
            }
            if (users.isNotEmpty()) {
                val mails = users.joinToString { it.email }
                throw ResourceAlreadyExistsException(
                    ErrorCode.ORG_DUPLICATE_USER,
                    "Some users are already a member of this organization: $mails",
                    mapOf("emails" to mails)
                )
            }
        }
    }
}
