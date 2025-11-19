package com.aryanspatel.moodmatch.backend

import com.aryanspatel.moodmatch.backend.config.configureAuth
import com.aryanspatel.moodmatch.backend.config.jwtSettings
import com.aryanspatel.moodmatch.backend.repository.UserRepository
import com.aryanspatel.moodmatch.backend.socket.configureSockets
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toIntOrNull() ?: 8080,
        host = "0.0.0.0"
    ) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()

    // Database first
    DatabaseFactory.init(environment.config)

    // Repositories
    val userRepo = UserRepository()

    // Auth
    configureAuth()
    val jwtSettings = jwtSettings()

    // WebSockets
    configureSockets(userRepo)

    // HTTP routes
    configureRouting(userRepo, jwtSettings)
}
