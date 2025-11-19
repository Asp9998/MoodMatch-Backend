package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import com.aryanspatel.moodmatch.backend.repository.UserRepository
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

suspend fun handleHeartbeat(
    session: DefaultWebSocketServerSession,
    userId: String,
    userRepo: UserRepository
) {
    // Update last seen
    userRepo.updateLastSeen(userId)

    val payload = buildJsonObject {
        put("userId", userId)
        put("ts", System.currentTimeMillis())
    }

    val envelop = WsEnvelopeDto(
        type = "heartbeat_ack",
        payload = payload
    )

    val text = WsJson.encodeToString(WsEnvelopeDto.serializer(), envelop)
    session.send(Frame.Text(text))
}