package com.jevzo.auth.model

import jakarta.persistence.*
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    val email: String,
    private var userPassword: String,
    @Enumerated(EnumType.STRING)
    var role: Role,
) : UserDetails {
    constructor() : this(null, "", "", Role.USER)
    constructor(email: String, userPassword: String) : this(null, email, userPassword, Role.USER)
    constructor(email: String, userPassword: String, role: Role) : this(null, email, userPassword, role)

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        mutableSetOf(SimpleGrantedAuthority("ROLE_$role"))

    override fun getPassword(): String = userPassword
    override fun getUsername(): String = email
}

enum class Role {
    USER,
    ADMIN
}