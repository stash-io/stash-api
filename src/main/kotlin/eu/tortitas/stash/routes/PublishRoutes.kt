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

fun Route.publishRoutes(application: Application) {
    val userService = application.provideUserService()
    val linkService = application.provideLinkService()
    val collectionService = application.provideCollectionService()

    route("/publish") {
        get("/links/list/user/{id}") {
            val user = userService.read(call.parameters["id"]!!.toInt())

            if (user?.id == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid user")
                return@get
            }

            val links = linkService.readByUserId(user.id)
            val collections = collectionService.readByUserId(user.id)

            call.respondHtml {
                head {
                    title { +"Links" }
                    link(rel = "stylesheet", href = "/styles.css")
                    meta(name = "viewport", content = "width=device-width, initial-scale=1")
                    unsafe {
                        +"""
                        <script defer>
                          document.addEventListener("scroll", () => {
                            const header = document.querySelector(".header");
                            const isFocusedOnSection = window.scrollY % window.innerHeight === 0;

                            if (isFocusedOnSection) {
                              header.classList.remove("header--scrolled");
                              return;
                            }

                            header.classList.add("header--scrolled");
                          });
                        </script>"""
                    }
                }

                body {
                    unsafe {
                    +"""
                    <header class="header">
                    <h1 class="logo">st&lt;a/&gt;sh</h1>

                    <div class="flex gap-3">
                    <a href="https://github.com/stash-io/stash_app/releases/latest" class="button flex items-center gap-2">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" preserveAspectRatio="xMidYMid" viewBox="0 0 256 150"><path fill="currentColor" d="M255.285 143.47c-.084-.524-.164-1.042-.251-1.56a128.119 128.119 0 0 0-12.794-38.288 128.778 128.778 0 0 0-23.45-31.86 129.166 129.166 0 0 0-22.713-18.005c.049-.08.09-.168.14-.25 2.582-4.461 5.172-8.917 7.755-13.38l7.576-13.068c1.818-3.126 3.632-6.26 5.438-9.386a11.776 11.776 0 0 0 .662-10.484 11.668 11.668 0 0 0-4.823-5.536 11.85 11.85 0 0 0-5.004-1.61 11.963 11.963 0 0 0-2.218.018 11.738 11.738 0 0 0-8.968 5.798c-1.814 3.127-3.628 6.26-5.438 9.386l-7.576 13.069c-2.583 4.462-5.173 8.918-7.755 13.38-.282.487-.567.973-.848 1.467-.392-.157-.78-.313-1.172-.462-14.24-5.43-29.688-8.4-45.836-8.4-.442 0-.879 0-1.324.006-14.357.143-28.152 2.64-41.022 7.12a119.434 119.434 0 0 0-4.42 1.642c-.262-.455-.532-.911-.79-1.367-2.583-4.462-5.173-8.918-7.755-13.38L65.123 15.25c-1.818-3.126-3.632-6.259-5.439-9.386A11.736 11.736 0 0 0 48.5.048 11.71 11.71 0 0 0 43.49 1.66a11.716 11.716 0 0 0-4.077 4.063c-.281.474-.532.967-.742 1.473a11.808 11.808 0 0 0-.365 8.188c.259.786.594 1.554 1.023 2.296a3973.32 3973.32 0 0 1 5.439 9.386c2.53 4.357 5.054 8.713 7.58 13.069 2.582 4.462 5.168 8.918 7.75 13.38.02.038.046.075.065.112A129.184 129.184 0 0 0 45.32 64.38a129.693 129.693 0 0 0-22.2 24.015 127.737 127.737 0 0 0-9.34 15.24 128.238 128.238 0 0 0-10.843 28.764 130.743 130.743 0 0 0-1.951 9.524c-.087.518-.167 1.042-.247 1.56A124.978 124.978 0 0 0 0 149.118h256c-.205-1.891-.449-3.77-.734-5.636l.019-.012Z"/><path fill="#202124" d="M194.59 113.712c5.122-3.41 5.867-11.3 1.661-17.62-4.203-6.323-11.763-8.682-16.883-5.273-5.122 3.41-5.868 11.3-1.662 17.621 4.203 6.322 11.764 8.682 16.883 5.272ZM78.518 108.462c4.206-6.321 3.46-14.21-1.662-17.62-5.123-3.41-12.68-1.05-16.886 5.27-4.203 6.323-3.458 14.212 1.662 17.622 5.122 3.41 12.683 1.05 16.886-5.272Z"/></svg>
                    Descargar</a>
                    </div>
                    </header>"""
                    }

                    main {
                        style = "padding: 1rem; padding-top: 5rem; display: flex; flex-direction: column; gap: 1rem; max-width: 800px; margin: 0 auto;"

                        div {
                            style = "display: flex; align-items: center; gap: 1rem;"

                            h1 {
                                style = "margin-bottom: 1rem; font-size: 1.4rem; font-weight: bold;"
                                +"${user.username} quiere compartir contigo ${links.size} enlaces"
                            }
                        }

                        div {
                            h2 {
                                style = "font-size: 1.2rem; font-weight: bold; margin-bottom: 0.8rem;"
                                +"Últimos links"
                            }

                            div {
                                style = "display: flex; flex-wrap: wrap; gap: 0.8rem; margin-bottom: 1rem;"

                                for (link in links.take(4).filter { it.published }) {
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

                        div {
                            div {
                                style = "display: flex; flex-wrap: wrap; flex-direction: column; gap: 0.8rem; margin-bottom: 1rem;"

                                for (collection in collections.filter { it.published }) {
                                    h3 {
                                        style = "font-weight: bold; font-size: 1.2rem;"
                                        +"${collection.title}"
                                    }

                                    p {
                                        +"${collection.description}"
                                    }

                                    div {
                                        style = "display: flex; flex-direction: column; gap: 0.5rem;"
                                        for (link in links.filter{ it.collectionId == collection.id }.filter { it.published }) {
                                            a(href = link.url, target = "_blank") {
                                                div {
                                                    classes = setOf("card")
                                                    h4 {
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
        }
    }
}
