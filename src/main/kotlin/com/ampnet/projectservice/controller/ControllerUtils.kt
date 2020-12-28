package com.ampnet.projectservice.controller

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.core.jwt.exception.TokenException
import org.springframework.http.CacheControl
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Duration

internal object ControllerUtils {

    val cacheControl: CacheControl = CacheControl.maxAge(Duration.ofMinutes(1))

    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
        SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
            ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")
}
