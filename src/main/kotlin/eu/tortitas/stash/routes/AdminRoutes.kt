package eu.tortitas.stash.routes

import eu.tortitas.stash.plugins.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.head
import kotlinx.html.*
import kotlinx.serialization.Serializable

fun Route.adminRoutes(application: Application) {
    val userService = application.provideUserService()
    val linkService = application.provideLinkService()

    route("/admin") {
        authenticate {
            install(RoleAdminPlugin) {
                route("/users") {
                    get("/list") {
                        val users = userService.readAll()
                        call.respond(HttpStatusCode.OK, users)
                    }

                    delete("/{id}") {
                        val user = userService.read(id = call.parameters["id"]!!.toInt())
                        if (user == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@delete
                        }

                        userService.delete(id = user.id!!)
                        call.respond(HttpStatusCode.NoContent)
                    }

                    put("/{id}/ban") {
                        val user = userService.read(id = call.parameters["id"]!!.toInt())
                        if (user == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }

                        userService.update(id = user.id!!, user = user.copy(role = "banned"))
                        call.respond(HttpStatusCode.NoContent)
                    }

                    put("/{id}/unban") {
                        val user = userService.read(id = call.parameters["id"]!!.toInt())
                        if (user == null) {
                            call.respond(HttpStatusCode.NotFound)
                            return@put
                        }

                        userService.update(id = user.id!!, user = user.copy(role = "tier1"))
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
