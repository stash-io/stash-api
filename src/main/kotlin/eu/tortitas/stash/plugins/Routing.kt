package eu.tortitas.stash.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String)

fun Application.configureRouting() {
    val jwtService = provideJwtService()
    val userService = provideUserService()

    // install(StatusPages) {
    //     exception<Throwable> { call, cause ->
    //         call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
    //     }
    // }
    //
    // [!] Throws:
    // io.ktor.server.application.DuplicatePluginException: Please make sure that you use unique
    // name for the plugin and don't install it twice. Conflicting application plugin is already
    // installed with the same key as `StatusPages`

    routing {
        get("/") { call.respondText("Hello World!") }

        staticFiles("/static", File("static"))

        post("/register") {
            val request = call.receive<RegisterRequest>()

            val user = userService.readByEmail(request.email)

            if (user != null) {
                call.respond(HttpStatusCode.Conflict, "User already exists")
                return@post
            }

            val id = userService.create(ExposedUser(request.email, request.password))
            call.respond(HttpStatusCode.Created, id)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = userService.readByEmail(request.email)

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                return@post
            }

            val token = jwtService.makeToken(user.email)
            call.respond(hashMapOf("token" to token))
        }
    }
}
