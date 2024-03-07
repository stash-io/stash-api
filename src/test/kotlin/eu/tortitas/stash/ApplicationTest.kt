package eu.tortitas.stash

import eu.tortitas.stash.plugins.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.MDC.put
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testPing() = testApplication {
        client.get("/api/ping").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("pong", bodyAsText())
        }
    }

    @Test
    fun testRegister() = testApplication {
        application {
            val database = getPostgresDatabase()
            transaction(database) {
                UserService.Users.deleteAll()
            }
        }

        client
                .post("/api/auth/register") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"email\":\"test@test.test\",\"password\":\"test\"}")
                }
                .apply { assertEquals(HttpStatusCode.Created, status) }
    }
    @Test
    fun testRegisterUserAlreadyExists() = testApplication {
        application {
            val database = getPostgresDatabase()
            transaction(database) {
                UserService.Users.deleteAll()
            }
        }

        client
            .post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("{\"email\":\"test@test.test\",\"password\":\"test\"}")
            }
            .apply { assertEquals(HttpStatusCode.Created, status) }

        client
            .post("/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody("{\"email\":\"test@test.test\",\"password\":\"test\"}")
            }
            .apply { assertEquals(HttpStatusCode.Conflict, status) }
    }

    @Test
    fun testLogin() = testApplication {
        application {
            runBlocking {
                val database = getPostgresDatabase()
                transaction(database) {
                    UserService.Users.deleteAll()
                }

                val userService = provideUserService()
                userService.create(ExposedUser("indatabase@test.com", "test"))
            }
        }

        client
                .post("/api/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"email\":\"indatabase@test.com\",\"password\":\"test\"}")
                }
                .apply {
                    assertEquals(HttpStatusCode.OK, status)

                    @Serializable data class Response(val token: String)
                    val bodyParsed = Json.decodeFromString<Response>(bodyAsText())
                    assertNotNull(bodyParsed.token)
                }
    }
}
