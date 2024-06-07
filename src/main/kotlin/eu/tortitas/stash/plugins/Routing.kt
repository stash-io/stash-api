package eu.tortitas.stash.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eu.tortitas.stash.routes.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.*

fun Application.configureRouting() {
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
        install(RoleBannedPlugin) {
            route("/api") {
                get("/ping") { call.respondText("pong") }
                authRoute(this@configureRouting)
                collectionsRoute(this@configureRouting)
                linksRoute(this@configureRouting)
                adminRoutes(this@configureRouting)
                stripeRoute(this@configureRouting)
            }

            publishRoutes(this@configureRouting)
        }
    }
}
