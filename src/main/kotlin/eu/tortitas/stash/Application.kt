package eu.tortitas.stash

import com.stripe.Stripe
import eu.tortitas.stash.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSockets()
    configureSerialization()
    configureMonitoring()
    configureSecurity()
    configureStatic()
    configureRouting()
    configureJobs()
}
