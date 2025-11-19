package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

suspend fun handleSendMessage(
    session: DefaultWebSocketServerSession,
    userId: String,
    envelope: WsEnvelopeDto
){
    val obj = envelope.payload?.jsonObject
    val roomId = obj?.get("roomId")?.jsonPrimitive?.contentOrNull
    val text = obj?.get("text")?.jsonPrimitive?.contentOrNull

    if(roomId.isNullOrBlank() || text.isNullOrBlank()){
        sendWsError(session, "BAD_REQUEST", "send_message requires roomId and text")
        return
    }

    val currentRoomId = userCurrentRoom[userId]
    if(currentRoomId == null || currentRoomId != roomId){
        sendWsError(session, "NOT_IN_ROOM", "User is not in this room")
        return
    }

    val room = rooms[roomId]
    if(room == null || room.status != "active"){
        sendWsError(session, "ROOM_NOT_ACTIVE", "Room not active")
    }

    val otherUserId = when(userId){
        room?.userAId -> room.userBId
        room?.userBId -> room.userAId
        else -> {
            sendWsError(session, "NOT_IN_ROOM", "User is not in this room ")
            return
        }
    }

    val ts = System.currentTimeMillis()
    val messageId = UUID.randomUUID().toString()

    val payload = buildJsonObject {
        put("randomId", roomId)
        put("senderId", userId)
        put("text", text)
        put ("messageId", messageId)
        put ("ts", ts)
    }

    val out = WsEnvelopeDto(
        type = "message",
        payload = payload
    )

    val jsonText = WsJson.encodeToString(WsEnvelopeDto.serializer(), out)

    // Send to both users (so sender get confirmation and receiver sees message)
    connections[userId]?.send(Frame.Text(jsonText))
    connections[otherUserId]?.send(Frame.Text(jsonText))

}