package eu.tortitas.stash.routes

import eu.tortitas.stash.plugins.ExposedCollection
import eu.tortitas.stash.plugins.provideCollectionService
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
data class CreateCollectionRequest(
    val title: String,
    val description: String,
    val published: Boolean
)

fun Route.collectionsRoute(application: Application) {
    val jwtService = application.provideJwtService()
    val userService = application.provideUserService()
    val collectionService = application.provideCollectionService()

    route("/collections") {
        authenticate() {
            post("/create") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@post
                }

                val request = call.receive<CreateCollectionRequest>()

                val id = collectionService.create(ExposedCollection(
                    title = request.title,
                    description = request.description,
                    published = request.published,
                    userId = user.id,
                    null
                ))

                call.respond(HttpStatusCode.Created, id)
            }

            get("/list") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@get
                }

                @Serializable data class Response(val collections: List<ExposedCollection>)

                val collections = collectionService.readByUserId(user.id)
                call.respond(HttpStatusCode.OK, Response(collections))
            }
        }
    }
}
