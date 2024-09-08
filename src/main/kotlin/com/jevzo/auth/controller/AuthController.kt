package com.jevzo.auth.controller

import com.jevzo.auth.request.AuthenticationRequest
import com.jevzo.auth.response.AuthenticationResponse
import com.jevzo.auth.response.RefreshAuthenticationResponse
import com.jevzo.auth.service.JwtService
import com.jevzo.auth.service.UserService
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val userService: UserService,
    private val jwtService: JwtService
) {

    @PostMapping("/login")
    fun createAuthenticationToken(@RequestBody authenticationRequest: AuthenticationRequest): ResponseEntity<AuthenticationResponse> {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(authenticationRequest.email, authenticationRequest.password)
        )

        val user = userService.loadUserByEmail(authenticationRequest.email)
        val jwt = jwtService.generateToken(user)
        val refreshToken = jwtService.generateRefreshToken(user)

        return ResponseEntity.ok(AuthenticationResponse(jwt, refreshToken, user.id!!, user.role))
    }

    @PostMapping("/refresh")
    fun refreshToken(@RequestHeader(HttpHeaders.AUTHORIZATION) token: String): ResponseEntity<RefreshAuthenticationResponse> {
        val email = jwtService.extractEmail(token)
        val user = userService.loadUserByEmail(email)
        val jwt = jwtService.generateToken(user)

        return ResponseEntity.ok(RefreshAuthenticationResponse(jwt))
    }
}