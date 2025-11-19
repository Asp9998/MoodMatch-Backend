package com.aryanspatel.moodmatch.backend.module

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WsEnvelopeDto(
    val type: String,
    val payload: JsonElement? = null
)

data class Room(
    val roomId: String,
    val mood: String,
    val userAId: String,
    val userBId: String,
    val startedAt: Long,
    var endedAt: Long? = null,
    var status: String = "active" // "active" | "ended"
)