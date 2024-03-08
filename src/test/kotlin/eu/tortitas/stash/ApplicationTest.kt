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
    fun testStatic() = testApplication {
        client.get("/index.html").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
