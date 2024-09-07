package com.jevzo.auth.response.mapper

import com.jevzo.auth.model.User
import com.jevzo.auth.response.UserResponse
import org.springframework.stereotype.Component
import java.util.function.Function

@Component
class UserResponseMapper : Function<User, UserResponse> {
    override fun apply(user: User): UserResponse {
        return UserResponse(user.id!!, user.email, user.role)
    }
}