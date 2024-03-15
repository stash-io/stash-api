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
    val wwwPathName = System.getProperty("user.dir") + "/www"

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