package com.aryanspatel.moodmatch.backend.repository

import com.aryanspatel.moodmatch.backend.DatabaseFactory
import com.aryanspatel.moodmatch.backend.Users
import com.aryanspatel.moodmatch.backend.module.User
import com.aryanspatel.moodmatch.backend.module.toUser
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID


class UserRepository {

    suspend fun createUSer(
        nickname: String,
        mood: String,
        avatar: String
    ): User {
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()

        DatabaseFactory.dbQuery {
            Users.insert {
                it[userId] = id
                it[Users.nickname] = nickname
                it[Users.mood] = mood
                it[Users.avatar] = avatar
                it[Users.status] = "idle"
                it[Users.createdAt] = now
                it[Users.lastSeenAt] = now
            }
        }

        return User(
            userId = id,
            nickname = nickname,
            mood = mood,
            avatar = avatar,
            status = "idle",
            createdAt = now,
            lastSeenAt = now
        )
    }

    suspend fun findUser(userId: String): User? {
        return DatabaseFactory.dbQuery {
            Users
                .selectAll().where { Users.userId eq userId }
                .singleOrNull()
                ?.toUser()
        }
    }

    suspend fun updateLastSeen(userId: String) {
        val now = Instant.now().toEpochMilli()
        DatabaseFactory.dbQuery {
            Users.update({ Users.userId eq userId }) {
                it[lastSeenAt] = now
            }
        }
    }

    suspend fun updateProfile(
        userId: String,
        nickname: String? = null,
        mood: String? = null,
        avatar: String? = null
    ): User? {
        return DatabaseFactory.dbQuery {
            val existing = Users
                .selectAll().where { Users.userId eq userId }
                .singleOrNull()
                ?.toUser()
                ?: return@dbQuery null

            val newNickname = nickname ?: existing.nickname
            val newMood = mood ?: existing.mood
            val newAvatar = avatar ?: existing.avatar
            val now = System.currentTimeMillis()

            Users.update({ Users.userId eq userId }) {
                it[Users.nickname] = newNickname
                it[Users.mood] = newMood
                it[Users.avatar] = newAvatar
                it[Users.lastSeenAt] = now
            }

            existing.copy(
                nickname = newNickname,
                mood = newMood,
                avatar = newAvatar,
                lastSeenAt = now
            )
        }
    }
}