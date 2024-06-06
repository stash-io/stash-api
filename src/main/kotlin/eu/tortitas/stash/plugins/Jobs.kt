package eu.tortitas.stash.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import eu.tortitas.stash.jobs.scheduleReportsJob
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

fun Application.configureJobs() {
    scheduleReportsJob()
}
