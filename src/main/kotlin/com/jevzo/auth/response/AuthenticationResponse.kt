package com.jevzo.auth.response

import com.jevzo.auth.model.Role

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String?,
    val userId: Long?,
    val role: Role?
)