package com.jevzo.auth.service

import com.jevzo.auth.exception.EmailExistsException
import com.jevzo.auth.exception.FailedToCreateUserException
import com.jevzo.auth.exception.UserEmailNotFoundException
import com.jevzo.auth.exception.UserIdNotFoundException
import com.jevzo.auth.model.User
import com.jevzo.auth.repository.UserRepository
import com.jevzo.auth.request.CreateUserRequest
import com.jevzo.auth.request.UpdateRoleRequest
import com.jevzo.auth.response.UserResponse
import com.jevzo.auth.response.mapper.UserResponseMapper
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val userResponseMapper: UserResponseMapper
) : UserDetailsService {

    fun createUser(createUserRequest: CreateUserRequest): UserResponse {
        userRepository.findByEmail(createUserRequest.email).ifPresent {
            throw EmailExistsException(createUserRequest.email)
        }

        val userToCreate = User(createUserRequest.email, passwordEncoder.encode(createUserRequest.password))
        userRepository.save(userToCreate)

        return userRepository.findByEmail(createUserRequest.email).map(userResponseMapper)
            .orElseThrow { FailedToCreateUserException("User with email ${createUserRequest.email} was not saved into the db") }
    }

    fun loadUserByEmail(email: String): User = userRepository.findByEmail(email)
        .orElseThrow { UserEmailNotFoundException(email) }

    fun getUser(id: Long): UserResponse {
        return userRepository.findById(id).map(userResponseMapper).orElseThrow { UserIdNotFoundException(id) }
    }

    fun getUsers(page: Int, count: Int): List<UserResponse> {
        return userRepository.findAll(PageRequest.of(page, count))
            .map(userResponseMapper)
            .toList()
    }

    @Transactional
    fun updateRole(updateRoleRequest: UpdateRoleRequest) {
        val user = userRepository.findById(updateRoleRequest.id).orElseThrow {
            UserIdNotFoundException(updateRoleRequest.id)
        }

        user.role = updateRoleRequest.role
    }

    fun deleteUser(id: Long) {
        val user = userRepository.findById(id).orElseThrow {
            UserIdNotFoundException(id)
        }

        userRepository.delete(user)
    }

    override fun loadUserByUsername(email: String): UserDetails = loadUserByEmail(email)
}