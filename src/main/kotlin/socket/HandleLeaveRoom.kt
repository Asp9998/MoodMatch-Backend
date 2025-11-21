package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

suspend fun leaveRoom(
    session: DefaultWebSocketServerSession,
    userId: String
){
    val roomId = userCurrentRoom[userId]

    if (roomId == null) {
        // Optional: send a small error or just ignore
        sendWsError(session, "NOT_IN_ROOM", "User is not currently in a room")
        return
    }

    // Reuse your existing logic for ending the room and notifying partner
    handleDisconnectFromRoom(userId)

    // Optional: send an ACK back to the leaver
    val payload = buildJsonObject {
        put("roomId", roomId)
    }

    val envelope = WsEnvelopeDto(
        type = "room_left",
        payload = payload
    )

    val text = WsJson.encodeToString(WsEnvelopeDto.serializer(), envelope)
    session.send(Frame.Text(text))

}