package com.jevzo.auth.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.jevzo.auth.model.Role
import com.jevzo.auth.model.User
import com.jevzo.auth.repository.UserRepository
import com.jevzo.auth.request.AuthenticationRequest
import com.jevzo.auth.response.AuthenticationResponse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    companion object {
        @Container
        val postgresContainer = PostgreSQLContainer<Nothing>("postgres:13").apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerPgProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
        }
    }

    @BeforeAll
    fun setup() {
        val users = listOf(
            User(
                email = "testuser@example.com",
                userPassword = passwordEncoder.encode("password"),
                role = Role.USER
            ),
            User(
                email = "testadmin@example.com",
                userPassword = passwordEncoder.encode("password"),
                role = Role.ADMIN
            )
        )
        userRepository.saveAll(users)
    }

    @Test
    fun `should authenticate and return JWT and refresh token`() {
        val authenticationRequest = AuthenticationRequest(
            email = "testuser@example.com",
            password = "password"
        )

        val response: MockHttpServletResponse = mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authenticationRequest))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response

        val authResponse = objectMapper.readValue(response.contentAsString, AuthenticationResponse::class.java)
        assert(authResponse.accessToken.isNotBlank()) { "JWT token should not be blank" }
        assert(!authResponse.refreshToken.isNullOrBlank()) { "Refresh token should not be null" }
        assert(authResponse.user != null) { "User info should not be null" }
    }

    @Test
    fun `should refresh JWT using refresh token`() {
        val authenticationRequest = AuthenticationRequest(
            email = "testuser@example.com",
            password = "password"
        )

        val loginResponse: MockHttpServletResponse = mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authenticationRequest))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response

        val authResponse = objectMapper.readValue(loginResponse.contentAsString, AuthenticationResponse::class.java)

        val refreshToken = authResponse.refreshToken!!
        val refreshResponse: MockHttpServletResponse = mockMvc.perform(
            MockMvcRequestBuilders.post("/auth/refresh")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $refreshToken")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn().response

        val newAuthResponse =
            objectMapper.readValue(refreshResponse.contentAsString, AuthenticationResponse::class.java)
        assert(newAuthResponse.accessToken.isNotBlank()) { "New JWT token should not be blank" }
    }
}