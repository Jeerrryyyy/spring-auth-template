package com.jevzo.auth.request

data class AuthenticationRequest(
    val email: String,
    val password: String
)