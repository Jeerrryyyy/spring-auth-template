package com.jevzo.auth.response

data class AuthenticationResponse(
    val accessToken: String,
    val refreshToken: String?
)