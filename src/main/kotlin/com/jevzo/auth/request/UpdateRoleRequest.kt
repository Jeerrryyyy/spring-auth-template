package com.jevzo.auth.request

import com.jevzo.auth.model.Role

data class UpdateRoleRequest(
    val id: Long,
    val role: Role
)