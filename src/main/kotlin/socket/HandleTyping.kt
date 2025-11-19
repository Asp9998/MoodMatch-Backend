package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

suspend fun handleTyping(
    userId: String,
    envelope: WsEnvelopeDto
){
    val obj = envelope.payload?.jsonObject
    val roomId = obj?.get("roomId")?.jsonPrimitive?.contentOrNull
    val isTyping = obj?.get("isTyping")?.jsonPrimitive?.contentOrNull

    if(roomId.isNullOrBlank() || isTyping == null){
        // Can't send error to a specific session here easily;
        return
    }

    val room = rooms[roomId] ?: return
    if(room.status != "active") return

    val otherUserId = when (userId){
        room.userAId -> room.userBId
        room.userBId -> room.userAId
        else -> return
    }

    val payload = buildJsonObject {
        put("roomId", roomId)
        put ("isTyping", isTyping)
    }

    val out = WsEnvelopeDto(
        type = "typing",
        payload = payload
    )

    val jsonText = WsJson.encodeToString(WsEnvelopeDto.serializer(), out)
    connections[otherUserId]?.send(Frame.Text(jsonText))
}