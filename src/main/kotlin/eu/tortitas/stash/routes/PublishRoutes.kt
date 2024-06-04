package eu.tortitas.stash.routes

import eu.tortitas.stash.plugins.ExposedLink
import eu.tortitas.stash.plugins.provideLinkService
import eu.tortitas.stash.plugins.provideJwtService
import eu.tortitas.stash.plugins.provideUserService
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

fun Route.publishRoutes(application: Application) {
    val userService = application.provideUserService()
    val linkService = application.provideLinkService()

    route("/publish") {
        get("/links/list/user/{id}") {
            val user = userService.read(call.parameters["id"]!!.toInt())

            if (user?.id == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                return@get
            }

            val links = linkService.readByUserId(call.parameters["id"]!!.toInt())

            call.respondHtml {
                head {
                    title { +"Links" }
                    link(rel = "stylesheet", href = "/styles.css")
                }

                body {
                    main {
                        style = "padding: 2rem; display: flex; flex-direction: column; align-items: center; gap: 1rem;"
                        h1 {
                            classes = setOf("heading")
                            +"${user.username}'s links"
                        }

                        div {
                            style = "display: flex; flex-direction: column; gap: 0.8rem;"
                            for (link in links) {
                                if (!link.published) {
                                    continue
                                }

                                a(href = link.url, target = "_blank") {
                                    div {
                                        classes = setOf("card")
                                        h2 {
                                            style = "font-weight: bold;"
                                            +"${link.title}"
                                        }
                                        h3 {
                                            +"${link.description}"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
