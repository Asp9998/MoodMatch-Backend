package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

suspend fun sendWsError(
    session: DefaultWebSocketServerSession,
    code: String,
    message: String
){
    val payload = buildJsonObject{
        put("code", code)
        put("message", message)
    }

    val envelope = WsEnvelopeDto(
        type = "error",
        payload = payload
    )

    val text = WsJson.encodeToString(WsEnvelopeDto.serializer(), envelope)
    session.send(Frame.Text(text))
}