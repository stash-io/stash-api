package eu.tortitas.stash.plugins

import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.io.File

fun Application.configureStatic() {
    handleNode()

    routing {
        staticFiles("/", File("www/dist"))
    }
}

fun handleNode() {
    installNodeDependenciesAndBuild()
    runBlocking {
        startBuildWatcher()
    }
}

fun installNodeDependenciesAndBuild() {
    ProcessBuilder("npm", "install").directory(File("www")).start().waitFor()
    ProcessBuilder("npm", "run", "build").directory(File("www")).start().waitFor()
}

suspend fun startBuildWatcher() = coroutineScope {
    launch {
        ProcessBuilder("npm", "run", "build:watch").directory(File("www")).start()
    }
}