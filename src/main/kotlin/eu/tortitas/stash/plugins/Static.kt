package eu.tortitas.stash.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import java.io.File

fun Application.configureStatic() {
    launch {
        ProcessBuilder("npm", "run", "build").directory(File("www")).start()
    }

    routing {
        staticFiles("/", File("www/dist"))
    }
}
