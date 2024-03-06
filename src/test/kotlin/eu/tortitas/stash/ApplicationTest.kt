package eu.tortitas.stash

import eu.tortitas.stash.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application { configureRouting() }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }

    @Test
    fun testLogin() = testApplication {
        application { configureRouting() }
        client
                .post("/login") {
                    contentType(ContentType.Application.Json)
                    setBody("{\"email\":\"notindatabase@test.com\",\"password\":\"test\"}")
                }
                .apply { assertEquals(HttpStatusCode.Unauthorized, status) }
    }
}
