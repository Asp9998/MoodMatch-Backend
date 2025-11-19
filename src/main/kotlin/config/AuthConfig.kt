package com.aryanspatel.moodmatch.backend.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.respond
import java.util.Date
/**
 * Holds JWT settings read from application.conf.
 */
data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
)

fun Application.jwtSettings(): JwtSettings {
    val config = environment.config.config("app.jwt")
    return JwtSettings(
        secret = config.property("secret").getString(),
        issuer = config.property("issuer").getString(),
        audience = config.property("audience").getString(),
        realm = config.property("realm").getString()
    )
}

/**
 * Install Ktor Authentication with JWT.
 */
fun Application.configureAuth() {
    val jwtSettings = jwtSettings()
    val algorithm = Algorithm.HMAC256(jwtSettings.secret)

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtSettings.realm

            verifier(
                JWT
                    .require(algorithm)
                    .withIssuer(jwtSettings.issuer)
                    .withAudience(jwtSettings.audience)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload.getClaim("uid").asString()
                if (userId.isNullOrBlank()) {
                    null
                } else {
                    // You could also check if user exists in UserStore here
                    JWTPrincipal(credential.payload)
                }
            }

            challenge { _, _ ->
                // Called when token is missing/invalid
                call.respondUnauthorized("Invalid or missing token")
            }
        }
    }
}

/**
 * Helper to generate JWT for a given userId.
 */
fun generateToken(userId: String, settings: JwtSettings): String {
    val now = System.currentTimeMillis()
    val oneYearMillis = 365L * 24 * 60 * 60 * 1000

    return JWT.create()
        .withAudience(settings.audience)
        .withIssuer(settings.issuer)
        .withSubject("auth")
        .withClaim("uid", userId)
        .withIssuedAt(Date(now))
        .withExpiresAt(Date(now + oneYearMillis)) // tweak expiry later if you want
        .sign(Algorithm.HMAC256(settings.secret))
}

/**
 * Small helper to send standardized unauthorized responses.
 */
suspend fun ApplicationCall.respondUnauthorized(message: String) {
    respond(
        io.ktor.http.HttpStatusCode.Unauthorized,
        mapOf("error" to "unauthorized", "message" to message)
    )
}