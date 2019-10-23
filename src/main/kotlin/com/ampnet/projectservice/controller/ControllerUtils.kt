package com.ampnet.projectservice.controller

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.core.jwt.exception.TokenException
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {
    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
            SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
                    ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")
}
