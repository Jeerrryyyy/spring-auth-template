package com.jevzo.auth.response

import com.jevzo.auth.model.Role

data class UserResponse(
    val id: Long,
    val email: String,
    val role: Role
)