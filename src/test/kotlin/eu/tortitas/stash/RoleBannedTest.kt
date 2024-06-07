package eu.tortitas.stash

import eu.tortitas.stash.plugins.*
import eu.tortitas.stash.routes.LoginRequest
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

class RoleBannedTest {
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
                createTestUser(userService, "banned")
            }
        }

        client
            .post("/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody("{\"email\":\"indatabase@test.com\",\"password\":\"test\"}")
            }
            .apply {
                assertEquals(HttpStatusCode.OK, status)

                val bodyParsed = Json.decodeFromString<LoginResponse>(bodyAsText())
                assertNotNull(bodyParsed.token)
                assertNotNull(bodyParsed.username)
                assertEquals(bodyParsed.username, "Test")

                client.get("/api/links/list") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${bodyParsed.token}")
                }.apply {
                    assertEquals(HttpStatusCode.Forbidden, status)
                }
            }
    }
}
