package eu.tortitas.stash.services

import io.ktor.util.logging.*

object LogService {
    private val logger = KtorSimpleLogger("LogService")

    fun log(message: String) {
        logger.info(message)
    }
}