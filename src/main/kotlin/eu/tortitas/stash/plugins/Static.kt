package eu.tortitas.stash.plugins

import eu.tortitas.stash.services.LogService
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import java.io.File

fun Application.configureStatic() {
    val isDevelopment = environment.config.propertyOrNull("ktor.development")?.getString() == "true"
    if (isDevelopment) {
        LogService.log("Running in development mode. Starting Node.js build watcher.")
        handleNode()
    }

    routing {
        staticFiles("/", File("www/dist"))
    }
}

fun handleNode() {
    val wwwPathName = "www"

    installNodeDependenciesAndBuild(wwwPathName)
    runBlocking {
        startBuildWatcher(wwwPathName)
    }
}

fun installNodeDependenciesAndBuild(wwwPathName: String) {
    val workingDirectory = File(wwwPathName)

    ProcessBuilder("npm", "install").directory(workingDirectory).start().waitFor()
    ProcessBuilder("npm", "run", "build").directory(workingDirectory).start().waitFor()
}

suspend fun startBuildWatcher(wwwPathName: String) = coroutineScope {
    launch {
        ProcessBuilder("npm", "run", "build:watch").directory(File(wwwPathName)).start()
    }
}