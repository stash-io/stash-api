package eu.tortitas.stash.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity() {
    // Please read the jwt property from the config file if you are using EngineMain
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtDomain = environment.config.property("jwt.domain").getString()
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtDomain)
                    .build()
            )
            validate { credential ->
                if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
            }
        }
    }
}


class JwtService (
    val secret: String,
    val issuer: String,
    val audience: String
) {
    fun makeToken(email: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("email", email)
            .sign(Algorithm.HMAC256(secret))
    }
}

fun Application.provideJwtService() = JwtService(
    environment.config.property("jwt.secret").getString(),
    environment.config.property("jwt.domain").getString(),
    environment.config.property("jwt.audience").getString()
)