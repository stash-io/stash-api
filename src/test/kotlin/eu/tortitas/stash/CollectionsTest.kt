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

class CollectionsTest {
    @Test
    fun testCreate() = testApplication {
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

                client.post("/api/collections/create") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${bodyParsed.token}")
                    setBody("{\"title\":\"My Collection\",\"description\":\"This is a collection\",\"published\":true}")
                }.apply {
                    assertEquals(HttpStatusCode.Created, status)
                }
            }
    }

    @Test
    fun testList() = testApplication {
        application {
            runBlocking {
                val database = getPostgresDatabase()
                transaction(database) {
                    LinkService.Links.deleteAll()
                    CollectionService.Collections.deleteAll()
                    UserService.Users.deleteAll()
                }

                val userService = provideUserService()
                val userId =createTestUser(userService)

                val collectionService = provideCollectionService()
                collectionService.create(ExposedCollection("My Collection", "This is a collection", true, userId, null))
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

                client.get("/api/collections/list") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer ${bodyParsed.token}")
                }.apply {
                    assertEquals(HttpStatusCode.OK, status)
                    @Serializable data class Response(val collections: List<ExposedCollection>)
                    val bodyParsed = Json.decodeFromString<Response>(bodyAsText())
                    assertNotNull(bodyParsed.collections)
                    assertEquals(bodyParsed.collections.size, 1)
                }
            }
    }
}
