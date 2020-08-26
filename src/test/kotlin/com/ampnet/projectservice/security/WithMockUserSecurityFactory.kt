package com.ampnet.projectservice.security

import com.ampnet.core.jwt.UserPrincipal
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import java.util.UUID

class WithMockUserSecurityFactory : WithSecurityContextFactory<WithMockCrowdfundUser> {

    companion object {
        const val fullName = "First Last"
    }

    private val password = "password"

    override fun createSecurityContext(annotation: WithMockCrowdfundUser): SecurityContext {
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

    private fun mapPrivilegesOrRoleToAuthorities(annotation: WithMockCrowdfundUser): List<SimpleGrantedAuthority> {
        return if (annotation.privileges.isNotEmpty()) {
            annotation.privileges.map { SimpleGrantedAuthority(it.name) }
        } else {
            emptyList()
        }
    }
}
