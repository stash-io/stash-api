package eu.tortitas.stash.routes

import eu.tortitas.stash.plugins.ExposedLink
import eu.tortitas.stash.plugins.provideLinkService
import eu.tortitas.stash.plugins.provideJwtService
import eu.tortitas.stash.plugins.provideUserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class CreateLinkRequest(
    val title: String,
    val description: String,
    val url: String?,
    val published: Boolean,
    val collectionId: Int?
)

@Serializable
data class UpdateLinkRequest(
    val id: Int,
    val title: String,
    val description: String,
    val url: String?,
    val published: Boolean,
    val collectionId: Int?
)

fun Route.linksRoute(application: Application) {
    val userService = application.provideUserService()
    val linkService = application.provideLinkService()

    route("/links") {
        authenticate() {
            post("/create") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@post
                }

                val request = call.receive<CreateLinkRequest>()

                val id = linkService.create(ExposedLink(
                    title = request.title,
                    description = request.description,
                    url = request.url,
                    published = request.published,
                    userId = user.id,
                    null,
                    collectionId = request.collectionId,
                    null
                ))

                call.respond(HttpStatusCode.Created, id)
            }

            put("/update") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@put
                }

                val request = call.receive<UpdateLinkRequest>()

                val id = linkService.update(request.id, user.id, ExposedLink(
                    title = request.title,
                    description = request.description,
                    url = request.url,
                    published = request.published,
                    userId = user.id,
                    null,
                    collectionId = request.collectionId,
                    null
                ))

                call.respond(HttpStatusCode.OK, id)
            }

            get("/list") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@get
                }

                @Serializable data class Response(val links: List<ExposedLink>)

                val links = linkService.readByUserId(user.id)
                call.respond(HttpStatusCode.OK, Response(links))
            }

            get("/{id}") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@get
                }

                @Serializable data class Response(val link: ExposedLink)

                val link = linkService.read(call.parameters["id"]!!.toInt(), user.id)

                if (link == null) {
                    call.respond(HttpStatusCode.NotFound, "No link found")
                    return@get
                }

                call.respond(HttpStatusCode.OK, Response(link))
            }

            delete("/{id}") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@delete
                }

                @Serializable data class Response(val link: ExposedLink)

                linkService.delete(call.parameters["id"]!!.toInt(), user.id)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
