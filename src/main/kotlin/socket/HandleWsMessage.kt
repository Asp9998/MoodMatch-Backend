package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import com.aryanspatel.moodmatch.backend.repository.UserRepository
import io.ktor.server.websocket.DefaultWebSocketServerSession
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun handleWsMessage(
    session: DefaultWebSocketServerSession,
    userId: String,
    text: String,
    userRepo: UserRepository
){
    val envelope = try{
        WsJson.decodeFromString(WsEnvelopeDto.serializer(), text)
    } catch (e: Exception){
        sendWsError(session, "BAD_ENVELOPE", "Invalid envelop: ${e.message}")
        return
    }

    when(envelope.type){
        "heartbeat" -> {
            handleHeartbeat(session, userId, userRepo)
        }

        "join_queue" -> {
            val mood = envelope.payload
                ?.jsonObject
                ?.get("mood")
                ?.jsonPrimitive
                ?.contentOrNull

            if(mood.isNullOrBlank()){
                sendWsError(session, "BAD_REQUEST", "join_queue requires mood")
                return
            }
            handleJoinQueue(session, userId, mood, userRepo)
        }

        "leave_queue" -> {
            handleLeaveQueue(session, userId)
        }

        "send_message" -> {
            handleSendMessage(session, userId, envelope)
        }

        "typing" -> {
            handleTyping(userId, envelope)
        }

        else -> {
            sendWsError(session, "UNKNOWN_CODE", "Unknown Ws typy: ${envelope.type}")
        }
    }
}
