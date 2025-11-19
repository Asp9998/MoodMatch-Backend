package com.aryanspatel.moodmatch.backend.socket

import com.aryanspatel.moodmatch.backend.module.Room
import com.aryanspatel.moodmatch.backend.module.WsEnvelopeDto
import com.aryanspatel.moodmatch.backend.repository.UserRepository
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

suspend fun handleJoinQueue(
    session: DefaultWebSocketServerSession,
    userId: String,
    mood: String,
    userRepo: UserRepository
) {
    // If already in a room, don't allow queue join
    val currentRoom = userCurrentRoom[userId]
    if(currentRoom != null){
        sendWsError(session, "ALREADY_IN_ROOM", "User is already in a room")
        return
    }

    // Remove from any other queue to avoid duplicates
    removeFromAllQueues(userId)

    val queue = queueByMood.getOrPut(mood) { ArrayDeque() }

    if(!queue.contains(userId)){
        queue.addLast(userId)
    }

    // try to match two users from this mood queue
    tryMatch(mood, userRepo)
}

suspend fun handleLeaveQueue(
    session: DefaultWebSocketServerSession,
    userId: String
){
    removeFromAllQueues(userId)

    val envelop = WsEnvelopeDto(
        type = "queue_left",
        payload = null
    )

    val text = WsJson.encodeToString(WsEnvelopeDto.serializer(), envelop)
    session.send(Frame.Text(text))
}

fun removeFromAllQueues(userId: String){
    queueByMood.values.forEach { queue ->
        queue.removeIf { it == userId }
    }
}

// try to match to users from the mood queue. and if found, create a room and notify both with match_found.
private suspend fun tryMatch(
    mood: String,
    userRepo: UserRepository
){
    val queue = queueByMood[mood] ?: return

    // Need at least 2 users to match
    if(queue.size < 2) return

    // Pop two userIds
    val userAId = queue.removeFirst()
    val userBId = queue.removeFirst()

    val sessionA = connections[userAId]
    val sessionB = connections[userBId]

    // If either user disconnected, just ignore this pair (they can re-join)
    if(sessionA == null || sessionB == null){
        return
    }

    val roomId = UUID.randomUUID().toString()
    val room = Room(
        roomId = roomId,
        mood = mood,
        userAId = userAId,
        userBId = userBId,
        startedAt = System.currentTimeMillis(),
    )

    rooms[roomId] = room
    userCurrentRoom[userAId] = roomId
    userCurrentRoom[userBId] = roomId

    // Fetch profiles for partner info
    val userA = userRepo.findUser(userAId)
    val userB = userRepo.findUser(userBId)

    // Notify A
    val payloadA = buildJsonObject {
        put("roomId", roomId)
        put("mood", mood)
        put("partnerId", userBId)
        if(userB != null){
            put("partnerNickname", userB.nickname)
            put("partnerAvatar", userB.avatar)
        }
    }

    val envelopA = WsEnvelopeDto(
        type = "match_found",
        payload = payloadA
    )

    val textA = WsJson.encodeToString(WsEnvelopeDto.serializer(), envelopA)
    sessionA.send(Frame.Text(textA))

    // Notify B
    val payloadB = buildJsonObject {
        put("roomId", roomId)
        put("mood", mood)
        put("partnerId", userBId)
        if(userA != null){
            put("partnerNickname", userA.nickname)
            put("partnerAvatar", userA.avatar)
        }
    }

    val envelopB = WsEnvelopeDto(
        type = "match_found",
        payload = payloadB
    )

    val textB = WsJson.encodeToString(WsEnvelopeDto.serializer(), envelopB)
    sessionA.send(Frame.Text(textB))
}