package com.aryanspatel.moodmatch.backend.socket

object Validation {
    private val allowedMoods = setOf("chill", "fire", "silly", "shy", "wild")

    fun validateNickname(nickname: String): String? {
        if (nickname.isBlank()) return "Nickname cannot be blank"
        if (nickname.length > 24) return "Nickname too long (max 24 chars)"
        return null
    }

//    fun validateMood(mood: String): String? {
//        if (!allowedMoods.contains(mood)) {
//            return "Invalid mood. Allowed: $allowedMoods"
//        }
//        return null
//    }

    fun validateAvatar(avatar: String): String? {
        if (avatar.isBlank()) return "Avatar cannot be blank"
        if (avatar.length > 4) return "Avatar too long"
        return null
    }
}