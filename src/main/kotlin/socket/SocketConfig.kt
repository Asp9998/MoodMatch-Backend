package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.Room
import com.aryanspatel.moodmatch.backend.repository.UserRepository
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds


// Simple JSON instance for WS
val WsJson = Json { ignoreUnknownKeys = true }

// Registry for active connections (userId -> session)
val connections = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

// mood -> queue of waiting userIds
val queueByMood = ConcurrentHashMap<String, ArrayDeque<String>>()

// roomId -> Room
val rooms = ConcurrentHashMap<String, Room>()

// userId -> RoomId (if currently in the room)
val userCurrentRoom = ConcurrentHashMap<String, String>()

fun Application.configureSockets(userRepo: UserRepository) {
    install(WebSockets) {
        pingPeriod = 30.seconds
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        authenticate("auth-jwt") {
        webSocket("/ws") { // websocketSession

            // 1) Get principal & userId from token
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("uid")?.asString()

            if (userId.isNullOrBlank()) {
                close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "Missing or invalid uid in token"
                    )
                )
                return@webSocket
            }

            // 2) Verify user exists in DB
            val user = userRepo.findUser(userId)
            if (user == null) {
                close(
                    CloseReason(
                        CloseReason.Codes.VIOLATED_POLICY,
                        "User not found"
                    )
                )
                return@webSocket
            }

            // 3) Register connection
            connections[userId] = this
            userRepo.updateLastSeen(userId)
            // TO DO later: mark user status = "idle" or "online" in DB

            try {
                // 4) Consume incoming frames
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleWsMessage(
                            session = this,
                            userId = userId,
                            text = text,
                            userRepo = userRepo
                        )
                    }
                }
            } finally {
                // 5) Cleanup on disconnect
                connections.remove(userId)
                removeFromAllQueues(userId)
                handleDisconnectFromRoom(userId)
            }
        }
    }
    }
}
