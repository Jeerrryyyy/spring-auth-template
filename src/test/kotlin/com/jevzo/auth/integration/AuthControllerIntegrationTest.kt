package com.jevzo.auth.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.jevzo.auth.model.Role
import com.jevzo.auth.model.User
import com.jevzo.auth.repository.UserRepository
import com.jevzo.auth.request.AuthenticationRequest
import com.jevzo.auth.response.AuthenticationResponse
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:13").apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
        }
    }

    @BeforeAll
    fun setup() {
        val users = listOf(
            User(email = "testuser@example.com", userPassword = passwordEncoder.encode("password"), role = Role.USER),
            User(email = "testadmin@example.com", userPassword = passwordEncoder.encode("password"), role = Role.ADMIN)
        )
        userRepository.saveAll(users)
    }

    @Nested
    inner class AuthenticationTests {
        @Test
        fun `should authenticate and return JWT and refresh token`() {
            val authenticationRequest = AuthenticationRequest(
                email = "testuser@example.com",
                password = "password"
            )

            val response = mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(authenticationRequest)
            }.andExpect {
                status { isOk() }
            }.andReturn().response

            val authResponse = objectMapper.readValue(response.contentAsString, AuthenticationResponse::class.java)
            authResponse.accessToken shouldNotBe null
            authResponse.refreshToken shouldNotBe null
            authResponse.user shouldNotBe null
        }

        @Test
        fun `should return unauthorized when no token is submitted`() {
            mockMvc.post("/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }

    @Nested
    inner class AuthorizationTests {
        @Test
        fun `should return forbidden when no required role is submitted`() {
            val authenticationRequest = AuthenticationRequest(
                email = "testuser@example.com",
                password = "password"
            )

            val loginResponse = mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(authenticationRequest)
            }.andExpect {
                status { isOk() }
            }.andReturn().response

            val authResponse = objectMapper.readValue(loginResponse.contentAsString, AuthenticationResponse::class.java)
            authResponse.accessToken shouldNotBe null

            mockMvc.get("/user") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${authResponse.accessToken}")
            }.andExpect {
                status { isForbidden() }
            }
        }

        @Test
        fun `should return a response when the right role is submitted`() {
            val authenticationRequest = AuthenticationRequest(
                email = "testadmin@example.com",
                password = "password"
            )

            val loginResponse = mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(authenticationRequest)
            }.andExpect {
                status { isOk() }
            }.andReturn().response

            val authResponse = objectMapper.readValue(loginResponse.contentAsString, AuthenticationResponse::class.java)
            authResponse.accessToken shouldNotBe null

            val usersResponse = mockMvc.get("/user") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${authResponse.accessToken}")
            }.andExpect {
                status { isOk() }
            }.andReturn().response

            val users = objectMapper.readValue(usersResponse.contentAsString, List::class.java)
            users.isNotEmpty() shouldBe true
        }
    }

    @Nested
    inner class TokenRefreshTests {
        @Test
        fun `should refresh JWT using refresh token`() {
            val authenticationRequest = AuthenticationRequest(
                email = "testuser@example.com",
                password = "password"
            )

            val loginResponse = mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(authenticationRequest)
            }.andExpect {
                status { isOk() }
            }.andReturn().response

            val authResponse = objectMapper.readValue(loginResponse.contentAsString, AuthenticationResponse::class.java)
            authResponse.refreshToken shouldNotBe null

            val refreshResponse = mockMvc.post("/auth/refresh") {
                header(HttpHeaders.AUTHORIZATION, "Bearer ${authResponse.refreshToken}")
            }.andExpect {
                status { isOk() }
            }.andReturn().response

            val newAuthResponse =
                objectMapper.readValue(refreshResponse.contentAsString, AuthenticationResponse::class.java)
            newAuthResponse.accessToken shouldNotBe null
            newAuthResponse.refreshToken shouldBe null
        }
    }
}