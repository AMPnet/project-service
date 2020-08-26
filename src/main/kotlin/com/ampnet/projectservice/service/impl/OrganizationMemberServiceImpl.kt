package com.ampnet.projectservice.service.impl

import com.ampnet.projectservice.enums.OrganizationRoleType
import com.ampnet.projectservice.exception.ErrorCode
import com.ampnet.projectservice.exception.ResourceAlreadyExistsException
import com.ampnet.projectservice.exception.ResourceNotFoundException
import com.ampnet.projectservice.persistence.model.OrganizationMembership
import com.ampnet.projectservice.persistence.model.Role
import com.ampnet.projectservice.persistence.repository.OrganizationMembershipRepository
import com.ampnet.projectservice.persistence.repository.RoleRepository
import com.ampnet.projectservice.service.OrganizationMemberService
import com.ampnet.projectservice.service.pojo.OrganizationMemberServiceRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class OrganizationMemberServiceImpl(
    private val membershipRepository: OrganizationMembershipRepository,
    private val roleRepository: RoleRepository
) : OrganizationMemberService {

    companion object : KLogging()

    private val adminRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_ADMIN.id) }
    private val memberRole: Role by lazy { roleRepository.getOne(OrganizationRoleType.ORG_MEMBER.id) }

    @Transactional
    override fun addUserToOrganization(
        userUuid: UUID,
        organizationUuid: UUID,
        role: OrganizationRoleType
    ): OrganizationMembership {
        // user can have only one membership(role) per one organization
        membershipRepository.findByOrganizationUuidAndUserUuid(organizationUuid, userUuid).ifPresent {
            throw ResourceAlreadyExistsException(
                ErrorCode.ORG_DUPLICATE_USER,
                "User ${it.userUuid} is already a member of this organization ${it.organizationUuid}"
            )
        }
        logger.debug { "Adding user: $userUuid to organization: $organizationUuid" }

        val membership = OrganizationMembership::class.java.getConstructor().newInstance()
        membership.organizationUuid = organizationUuid
        membership.userUuid = userUuid
        membership.role = getRole(role)
        membership.createdAt = ZonedDateTime.now()
        return membershipRepository.save(membership)
    }

    @Transactional
    override fun removeUserFromOrganization(userUuid: UUID, organizationUuid: UUID) {
        membershipRepository.findByOrganizationUuidAndUserUuid(organizationUuid, userUuid).ifPresent {
            logger.debug { "Removing user: $userUuid from organization: $organizationUuid" }
            membershipRepository.delete(it)
        }
    }

    @Transactional(readOnly = true)
    override fun getOrganizationMemberships(organizationUuid: UUID): List<OrganizationMembership> {
        return membershipRepository.findByOrganizationUuid(organizationUuid)
    }

    @Transactional
    override fun updateOrganizationRole(request: OrganizationMemberServiceRequest) {
        logger.debug { "Updating organization role for user: ${request.memberUuid} from organization: $request.organizationUuid" }
        val membership = ServiceUtils.wrapOptional(
            membershipRepository.findByOrganizationUuidAndUserUuid(request.organizationUuid, request.memberUuid)
        ) ?: throw ResourceNotFoundException(
            ErrorCode.ORG_MEM_MISSING,
            "User ${request.memberUuid} is not a member of this organization ${request.organizationUuid}"
        )
        membership.role = getRole(request.roleType)
    }

    private fun getRole(role: OrganizationRoleType): Role {
        return when (role) {
            OrganizationRoleType.ORG_ADMIN -> adminRole
            OrganizationRoleType.ORG_MEMBER -> memberRole
        }
    }
}
