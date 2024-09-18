package com.jevzo.auth.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.jevzo.auth.model.Role
import com.jevzo.auth.model.User
import com.jevzo.auth.repository.UserRepository
import com.jevzo.auth.request.CreateUserRequest
import com.jevzo.auth.request.UpdateRoleRequest
import com.jevzo.auth.response.UserResponse
import io.kotest.matchers.shouldBe
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
import org.springframework.test.web.servlet.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    private lateinit var adminToken: String
    private lateinit var userToken: String

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

        adminToken = getAuthToken("testadmin@example.com", "password")
        userToken = getAuthToken("testuser@example.com", "password")
    }

    private fun getAuthToken(email: String, password: String): String {
        val loginResponse = mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password))
        }.andExpect { status { isOk() } }.andReturn().response

        return objectMapper.readValue(loginResponse.contentAsString, Map::class.java)["accessToken"] as String
    }

    @Nested
    inner class UserCreationTests {
        @Test
        fun `should create a new user`() {
            val email = "newuser@example.com"
            val createUserRequest = CreateUserRequest(email, "password")

            try {
                val result = mockMvc.post("/user/create") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(createUserRequest)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.email") { value(email) }
                    jsonPath("$.role") { value("USER") }
                }.andReturn()

                val createdUser = objectMapper.readValue(result.response.contentAsString, UserResponse::class.java)
                createdUser.email shouldBe email
                createdUser.role shouldBe Role.USER
            } finally {
                userRepository.findByEmail(email).ifPresent { userRepository.delete(it) }
            }
        }
    }

    @Nested
    inner class UserRetrievalTests {
        @Test
        fun `should get user by id when admin`() = testUserRetrieval(adminToken, "testuser@example.com", true)

        @Test
        fun `should get user by id when it's the user's own id`() =
            testUserRetrieval(userToken, "testuser@example.com", true)

        @Test
        fun `should not get user by id when not admin and not own id`() =
            testUserRetrieval(userToken, "testadmin@example.com", false)

        private fun testUserRetrieval(token: String, email: String, expectSuccess: Boolean) {
            val userId = userRepository.findByEmail(email).get().id

            mockMvc.get("/user/$userId") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            }.andExpect {
                if (expectSuccess) {
                    status { isOk() }
                    jsonPath("$.email") { value(email) }
                } else {
                    status { isForbidden() }
                }
            }
        }

        @Test
        fun `should get users when admin`() {
            mockMvc.get("/user") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            }.andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$[0].email") { exists() }
            }
        }

        @Test
        fun `should not get users when not admin`() {
            mockMvc.get("/user") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
            }.andExpect { status { isForbidden() } }
        }
    }

    @Nested
    inner class UserUpdateTests {
        @Test
        fun `should update role when admin`() {
            val userId = userRepository.findByEmail("testuser@example.com").get().id
            val updateRoleRequest = UpdateRoleRequest(userId!!, Role.ADMIN)

            mockMvc.patch("/user/role") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRoleRequest)
            }.andExpect { status { isNoContent() } }

            mockMvc.get("/user/$userId") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            }.andExpect {
                status { isOk() }
                jsonPath("$.role") { value("ADMIN") }
            }
        }

        @Test
        fun `should not update role when not admin`() {
            val userId = userRepository.findByEmail("testuser@example.com").get().id
            val updateRoleRequest = UpdateRoleRequest(userId!!, Role.ADMIN)

            mockMvc.patch("/user/role") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $userToken")
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(updateRoleRequest)
            }.andExpect { status { isForbidden() } }
        }
    }

    @Nested
    inner class UserDeletionTests {
        @Test
        fun `should delete user when admin`() = testUserDeletion(adminToken, "todelete@example.com", true)

        @Test
        fun `should delete user when it's the user's own id`() = testUserDeletion(null, "selfdelete@example.com", true)

        @Test
        fun `should not delete user when not admin and not own id`() =
            testUserDeletion(userToken, "testadmin@example.com", false)

        private fun testUserDeletion(token: String?, email: String, expectSuccess: Boolean) {
            val newUser = if (!userRepository.findByEmail(email).isPresent) {
                userRepository.save(
                    User(
                        email = email,
                        userPassword = passwordEncoder.encode("password"),
                        role = Role.USER
                    )
                )
            } else userRepository.findByEmail(email).get()

            val deleteToken = token ?: getAuthToken(email, "password")

            mockMvc.delete("/user/${newUser.id}") {
                header(HttpHeaders.AUTHORIZATION, "Bearer $deleteToken")
            }.andExpect {
                if (expectSuccess) {
                    status { isNoContent() }
                } else {
                    status { isForbidden() }
                }
            }

            if (expectSuccess) {
                mockMvc.get("/user/${newUser.id}") {
                    header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                }.andExpect { status { isNotFound() } }
            }
        }
    }
}