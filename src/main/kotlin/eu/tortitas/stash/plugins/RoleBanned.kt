package eu.tortitas.stash.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val RoleBannedPlugin = createRouteScopedPlugin(name = "RoleBannedPlugin") {
    val userService = application.provideUserService()

    pluginConfig.apply {
        on(AuthenticationChecked) { call ->
            val principal =
                call.authentication.principal<JWTPrincipal>() ?: return@on
            val user = userService.readByEmail(principal.payload.getClaim("email").asString())
            if (user == null) {
                call.respond(HttpStatusCode.Forbidden)
                return@on
            }

            val role = user.role
            if (role == "banned") {
                call.respond(HttpStatusCode.Forbidden)
                return@on
            }
        }
    }
}