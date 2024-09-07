package com.jevzo.auth.controller

import com.jevzo.auth.request.CreateUserRequest
import com.jevzo.auth.request.UpdateRoleRequest
import com.jevzo.auth.response.UserResponse
import com.jevzo.auth.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/user")
class UserController(
    private val userService: UserService
) {

    @PostMapping("/create")
    fun createUser(@RequestBody createUserRequest: CreateUserRequest): UserResponse {
        return userService.createUser(createUserRequest)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    fun getUser(@PathVariable id: Long): UserResponse {
        return userService.getUser(id)
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getUsers(
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "count", required = false, defaultValue = "10") count: Int
    ): ResponseEntity<List<UserResponse>> {
        return ResponseEntity.ok(userService.getUsers(page, count))
    }

    @PatchMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateRole(
        @RequestBody updateRoleRequest: UpdateRoleRequest
    ): ResponseEntity<Any> {
        userService.updateRole(updateRoleRequest)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal")
    fun deleteUser(
        @PathVariable id: Long
    ): ResponseEntity<Any> {
        userService.deleteUser(id)
        return ResponseEntity.noContent().build()
    }
}