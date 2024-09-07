package com.jevzo.auth.security

import com.jevzo.auth.model.Role
import com.jevzo.auth.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtRequestFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        httpServletRequest: HttpServletRequest,
        httpServletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authorizationHeader = httpServletRequest.getHeader("Authorization")

        var id: Long? = null
        var jwt: String? = null
        var role: Role? = null

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7)
            id = jwtService.extractId(jwt)
            role = jwtService.extractRole(jwt)
        }

        if (id != null && jwt != null && SecurityContextHolder.getContext().authentication == null) {
            if (jwtService.validateToken(jwt)) {
                val authenticationToken = jwtService.getAuthentication(id, role)
                authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(httpServletRequest)

                SecurityContextHolder.getContext().authentication = authenticationToken
            }
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse)
    }
}