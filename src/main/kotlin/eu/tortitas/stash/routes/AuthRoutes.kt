package eu.tortitas.stash.routes

import eu.tortitas.stash.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val username: String, val email: String, val password: String)

fun Route.authRoute(application: Application) {
    val jwtService = application.provideJwtService()
    val userService = application.provideUserService()

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()

            val user = userService.readByEmail(request.email)

            if (user != null) {
                call.respond(HttpStatusCode.Conflict, "User already exists")
                return@post
            }

            val id = userService.create(ExposedUser(request.username, request.email, request.password, "free", null, null))
            call.respond(HttpStatusCode.Created, id)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = userService.readByEmail(request.email)

            if (user == null || user.password != request.password) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user or password")
                return@post
            }

            val token = jwtService.makeToken(user.email)
            call.respond(hashMapOf("token" to token, "username" to user.username, "id" to user.id.toString(), "role" to user.role))
        }

        authenticate {
            get("/refresh") {
                val user =
                    userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid token")
                    return@get
                }

                val newToken = jwtService.makeToken(user.email)
                call.respond(hashMapOf("token" to newToken, "username" to user.username, "id" to user.id.toString(), "role" to user.role, "email" to user.email))
            }
        }
    }
}