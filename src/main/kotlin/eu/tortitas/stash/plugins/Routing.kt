package eu.tortitas.stash.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.*
import kotlinx.serialization.Serializable

@Serializable data class LoginRequest(val email: String, val password: String)

fun Application.configureRouting() {
    val secret = environment.config.property("jwt.secret").getString()
    val issuer = environment.config.property("jwt.domain").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val myRealm = environment.config.property("jwt.realm").getString()

    val database = getPostgresDatabase()
    val userService = UserService(database)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") { call.respondText("Hello World!") }
        static("/static") { resources("static") }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = userService.readByEmail(request.email)

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                return@post
            }

            val token =
                    JWT.create()
                            .withAudience(audience)
                            .withIssuer(issuer)
                            .withClaim("email", user?.email ?: "a")
                            .withExpiresAt(Date(System.currentTimeMillis() + 60000))
                            .sign(Algorithm.HMAC256(secret))
            call.respond(hashMapOf("token" to token))
        }
    }
}
