package com.jevzo.auth.service

import com.jevzo.auth.model.Role
import com.jevzo.auth.model.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Service
class JwtService(
    @Value("\${spring.jwt.secret}")
    private val jwtSecret: String
) {

    fun extractId(token: String): Long = extractClaim(token, Claims::getSubject).toLong()
    fun extractRole(token: String): Role? = when (extractAllClaims(token)["role"]) {
        "USER" -> Role.USER
        "ADMIN" -> Role.ADMIN
        else -> null
    }

    fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver.invoke(claims)
    }

    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(this.getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun validateToken(token: String): Boolean = !isTokenExpired(token)

    private fun isTokenExpired(token: String): Boolean {
        return extractExpiration(token).before(Date())
    }

    fun extractExpiration(token: String): Date {
        return extractClaim(token, Claims::getExpiration)
    }

    fun generateToken(user: User): String {
        val claims: MutableMap<String, Any> = mutableMapOf("role" to user.role)
        return createToken(claims, user.id!!, Date(System.currentTimeMillis() + 1000 * 60 * 10))
    }

    private fun createToken(claims: Map<String, Any>, subject: Long, expiration: Date): String {
        return Jwts.builder()
            .claims(claims)
            .subject(subject.toString())
            .issuedAt(Date(System.currentTimeMillis()))
            .expiration(expiration)
            .signWith(this.getSigningKey())
            .compact()
    }

    fun generateRefreshToken(user: User): String {
        val claims: MutableMap<String, Any> = HashMap()
        return createToken(claims, user.id!!, Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
    }

    fun getAuthentication(id: Long, role: Role?): UsernamePasswordAuthenticationToken {
        val authorities = if (role == null) listOf()
        else listOf(SimpleGrantedAuthority("ROLE_$role"))

        return UsernamePasswordAuthenticationToken(id, null, authorities)
    }

    fun getSigningKey(): SecretKey {
        val keyBuffer = Decoders.BASE64.decode(jwtSecret)
        return SecretKeySpec(keyBuffer, "HmacSHA256")
    }
}