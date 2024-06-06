package eu.tortitas.stash

import at.favre.lib.crypto.bcrypt.BCrypt
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

public suspend fun createTestUser(userService: UserService, role: String = "tier1"): Int {
    return userService.create(ExposedUser("Test", "indatabase@test.com", BCrypt.withDefaults().hashToString(12, "test".toCharArray()), role, null, null, null))
}
@Serializable public data class LoginResponse(val token: String, val username: String, val id: String, val role: String, val reminderDayOfWeek: String?)

class AuthTest {
    @Test
    fun testRegister() = testApplication {
        application {
            val database = getPostgresDatabase()
            transaction(database) {
                LinkService.Links.deleteAll()
                CollectionService.Collections.deleteAll()
                UserService.Users.deleteAll()
            }
        }

        client
            .post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("{\"username\":\"Test\",\"email\":\"test@test.test\",\"password\":\"test\"}")
            }
            .apply { assertEquals(HttpStatusCode.Created, status) }
    }
    @Test
    fun testRegisterUserAlreadyExists() = testApplication {
        application {
            val database = getPostgresDatabase()
            transaction(database) {
                LinkService.Links.deleteAll()
                CollectionService.Collections.deleteAll()
                UserService.Users.deleteAll()
            }
        }

        client
            .post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("{\"username\":\"Test\",\"email\":\"test@test.test\",\"password\":\"test\"}")
            }
            .apply { assertEquals(HttpStatusCode.Created, status) }


        client
            .post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("{\"username\":\"Test\",\"email\":\"test@test.test\",\"password\":\"test\"}")
            }
            .apply { assertEquals(HttpStatusCode.Conflict, status) }
    }

    @Test
    fun testLogin() = testApplication {
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

                val bodyParsed = Json.decodeFromString<LoginResponse>(bodyAsText())
                assertNotNull(bodyParsed.token)
                assertNotNull(bodyParsed.username)
                assertEquals(bodyParsed.username, "Test")
            }
    }
}
