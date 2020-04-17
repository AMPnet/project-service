package com.ampnet.projectservice.security

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.projectservice.enums.PrivilegeType
import java.util.UUID
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory

class WithMockUserSecurityFactory : WithSecurityContextFactory<WithMockCrowdfoundUser> {

    companion object {
        const val fullName = "First Last"
    }

    private val password = "password"

    override fun createSecurityContext(annotation: WithMockCrowdfoundUser): SecurityContext {
        val authorities = mapPrivilegesOrRoleToAuthorities(annotation)
        val userPrincipal = UserPrincipal(
            UUID.fromString(annotation.uuid),
            annotation.email,
            fullName,
            authorities.asSequence().map { it.authority }.toSet(),
            annotation.enabled,
            annotation.verified
        )
        val token = UsernamePasswordAuthenticationToken(userPrincipal, password, authorities)

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        return context
    }

    private fun mapPrivilegesOrRoleToAuthorities(annotation: WithMockCrowdfoundUser): List<SimpleGrantedAuthority> {
        return if (annotation.privileges.isNotEmpty()) {
            annotation.privileges.map { SimpleGrantedAuthority(it.name) }
        } else {
            getDefaultUserPrivileges().map { SimpleGrantedAuthority(it.name) }
        }
    }

    private fun getDefaultUserPrivileges() = listOf(
            PrivilegeType.PRO_PROFILE,
            PrivilegeType.PWO_PROFILE,
            PrivilegeType.PRO_ORG_INVITE,
            PrivilegeType.PWO_ORG_INVITE)
}
