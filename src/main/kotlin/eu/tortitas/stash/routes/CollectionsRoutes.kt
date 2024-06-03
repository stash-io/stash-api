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

@Serializable
data class UpdateCollectionRequest(
    val id: Int,
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

            put("/update") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@put
                }

                val request = call.receive<UpdateCollectionRequest>()

                val id = collectionService.update(request.id, user.id, ExposedCollection(
                    title = request.title,
                    description = request.description,
                    published = request.published,
                    userId = user.id,
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

                @Serializable data class Response(val collections: List<ExposedCollection>)

                val collections = collectionService.readByUserId(user.id)
                call.respond(HttpStatusCode.OK, Response(collections))
            }

            get("/{id}") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@get
                }

                @Serializable data class Response(val collection: ExposedCollection)

                val collection = collectionService.read(call.parameters["id"]!!.toInt(), user.id)

                if (collection == null) {
                    call.respond(HttpStatusCode.NotFound, "No collection found")
                    return@get
                }

                call.respond(HttpStatusCode.OK, Response(collection))
            }

            delete("/{id}") {
                val user = userService.readByEmail(call.principal<JWTPrincipal>()!!.payload.getClaim("email").asString())

                if (user?.id == null) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                    return@delete
                }

                @Serializable data class Response(val collection: ExposedCollection)

                collectionService.delete(call.parameters["id"]!!.toInt(), user.id)

                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
