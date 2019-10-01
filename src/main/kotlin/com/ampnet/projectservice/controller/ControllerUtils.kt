package com.ampnet.projectservice.controller

import com.ampnet.projectservice.config.auth.UserPrincipal
import com.ampnet.projectservice.exception.TokenException
import org.springframework.security.core.context.SecurityContextHolder

internal object ControllerUtils {
    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
            SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
                    ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")
}
