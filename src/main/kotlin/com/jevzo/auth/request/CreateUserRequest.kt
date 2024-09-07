package com.jevzo.auth.request

data class CreateUserRequest(
    val email: String,
    val password: String
)