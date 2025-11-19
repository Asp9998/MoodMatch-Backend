package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

suspend fun handleDisconnectFromRoom(userId: String){
    
    val roomId = userCurrentRoom.remove(userId) ?: return
    val room = rooms[roomId] ?: return
    
    if(room.status == "ended") return
    
    room.status = "ended"
    room.endedAt = System.currentTimeMillis()
    
    val otherUserId = when (userId){
        room.userAId -> room.userBId
        room.userBId -> room.userAId
        else -> null
    } ?: return
    
    userCurrentRoom.remove(otherUserId)
    
    val payload = buildJsonObject { 
        put("roomId", roomId)
    }
    
    val out = WsEnvelopeDto(
        type = "partner_left",
        payload = payload
    )

    val jsonText = WsJson.encodeToString(WsEnvelopeDto.serializer(), out)

    connections[otherUserId]?.send(Frame.Text(jsonText))
}