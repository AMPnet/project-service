package com.ampnet.projectservice.controller

import com.ampnet.core.jwt.UserPrincipal
import com.ampnet.core.jwt.exception.TokenException
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal object ControllerUtils {
    fun getUserPrincipalFromSecurityContext(): UserPrincipal =
        SecurityContextHolder.getContext().authentication.principal as? UserPrincipal
            ?: throw TokenException("SecurityContext authentication principal must be UserPrincipal")

    fun LocalDate.toEpochMili(time: LocalTime): Long =
        LocalDateTime.of(this, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
