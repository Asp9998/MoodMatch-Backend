package com.aryanspatel.moodmatch.backend.module

import com.aryanspatel.moodmatch.backend.Users
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow

@Serializable
data class UserProfile(
    val userId: String,
    val nickname: String,
    val mood: String,
    val avatar: String,
    val status: String, // "offline" | "idle" | "in_queue" | "in_room"
    val createdAt: Long,
    val lastSeenAt: Long
)

// Internet representation
data class User(
    val userId: String,
    var nickname: String,
    var mood: String,
    var avatar: String,
    var status: String,
    val createdAt: Long,
    var lastSeenAt: Long
) {
    fun toProfile(): UserProfile = UserProfile(
        userId = userId,
        nickname = nickname,
        mood = mood,
        avatar = avatar,
        status = status,
        createdAt = createdAt,
        lastSeenAt = lastSeenAt
    )
}

@Serializable
data class RegisterRequest(
    val nickname: String,
    val mood: String,
    val avatar: String
)

@Serializable
data class RegisterResponse(
    val userId: String,
    val authToken: String,
    val profile: UserProfile
)

/**
 * Map Exposed ResultRow -> User
 */
fun ResultRow.toUser(): User = User(
    userId = this[Users.userId],
    nickname = this[Users.nickname],
    mood = this[Users.mood],
    avatar = this[Users.avatar],
    status = this[Users.status],
    createdAt = this[Users.createdAt],
    lastSeenAt = this[Users.lastSeenAt]
)

@Serializable
data class UpdatePreferencesRequest(
    val nickname: String? = null,
    val mood: String? = null,
    val avatar: String? = null
)