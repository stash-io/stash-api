package eu.tortitas.stash

import eu.tortitas.stash.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RoleAdminTest {
    @Test
    fun testProtectedRouteWithoutRole() = testApplication {
        application {
            runBlocking {
                val database = getPostgresDatabase()
                transaction(database) {
                    LinkService.Links.deleteAll()
                    CollectionService.Collections.deleteAll()
                    UserService.Users.deleteAll()
                }

                val userService = provideUserService()
                createTestUser(userService)
            }
        }

        client
            .post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("{\"email\":\"indatabase@test.com\",\"password\":\"test\"}")
            }
            .apply {
                assertEquals(HttpStatusCode.OK, status)

                @Serializable data class Response(val token: String, val username: String, val id: String, val role: String)
                val bodyParsed = Json.decodeFromString<Response>(bodyAsText())
                assertNotNull(bodyParsed.token)
                assertNotNull(bodyParsed.username)
                assertEquals(bodyParsed.username, "Test")

                client.get("/api/admin/users/list") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${bodyParsed.token}")
                }.apply {
                    assertEquals(HttpStatusCode.Forbidden, status)
                }
            }
    }

    @Test
    fun testProtectedRouteWithRole() = testApplication {
        application {
            runBlocking {
                val database = getPostgresDatabase()
                transaction(database) {
                    LinkService.Links.deleteAll()
                    CollectionService.Collections.deleteAll()
                    UserService.Users.deleteAll()
                }

                val userService = provideUserService()
                userService.create(ExposedUser("Test", "indatabase@test.com", "test", "admin", null, null))
            }
        }

        client
            .post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("{\"email\":\"indatabase@test.com\",\"password\":\"test\"}")
            }
            .apply {
                assertEquals(HttpStatusCode.OK, status)

                @Serializable data class Response(val token: String, val username: String, val id: String, val role: String)
                val bodyParsed = Json.decodeFromString<Response>(bodyAsText())
                assertNotNull(bodyParsed.token)
                assertNotNull(bodyParsed.username)
                assertEquals(bodyParsed.username, "Test")

                client.get("/api/admin/users/list") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${bodyParsed.token}")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                }
            }
    }
}
