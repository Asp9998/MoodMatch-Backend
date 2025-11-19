package com.aryanspatel.moodmatch.backend

import com.aryanspatel.moodmatch.backend.config.JwtSettings
import com.aryanspatel.moodmatch.backend.config.generateToken
import com.aryanspatel.moodmatch.backend.module.RegisterRequest
import com.aryanspatel.moodmatch.backend.module.RegisterResponse
import com.aryanspatel.moodmatch.backend.module.UpdatePreferencesRequest
import com.aryanspatel.moodmatch.backend.repository.UserRepository
import com.aryanspatel.moodmatch.backend.socket.Validation
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.request.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    userRepo: UserRepository,
    jwtSettings: JwtSettings
) {
    routing {
        get("/api/health") {
            call.respond(
                mapOf(
                    "status" to "ok",
                    "env" to (environment.config.propertyOrNull("app.env")?.getString() ?: "unknown")
                )
            )
        }

        authenticate("auth-jwt") {
            get("/api/users/me") {
                val principal = call.principal<JWTPrincipal>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                    return@get
                }

                val userId = principal.payload.getClaim("uid").asString()
                val user = userRepo.findUser(userId)

                if (user == null) {
                    // Token is valid structurally, but user no longer exists
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "user_not_found", "message" to "User does not exist")
                    )
                    return@get
                }
                userRepo.updateLastSeen(userId)
                call.respond(user.toProfile())
            }

            put("/api/users/me"){
                val principal = call.principal<JWTPrincipal>()
                if (principal == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized"))
                    return@put
                }

                val userId = principal.payload.getClaim("uid").asString()

                val request = try{
                    call.receive<UpdatePreferencesRequest>()
                } catch (e: Exception){
                    call.respond(
                        BadRequest,
                        mapOf("error" to "bad_request", "message" to "Invalid body: $e")
                    )
                    return@put
                }

                // If nothing provided, just return current state
                if (request.nickname == null && request.mood == null && request.avatar == null) {
                    val current = userRepo.findUser(userId)
                    if (current == null) {
                        call.respond(
                            HttpStatusCode.Unauthorized,
                            mapOf("error" to "user_not_found")
                        )
                    } else {
                        call.respond(current.toProfile())
                    }
                    return@put
                }

                // Validate only fields that are present
                request.nickname?.let { nick ->
                    Validation.validateNickname(nick)?.let { msg ->
                        call.respond(
                            BadRequest,
                            mapOf("error" to "invalid_nickname", "message" to msg)
                        )
                        return@put
                    }
                }
//                request.mood?.let { mood ->
//                    Validation.validateMood(mood)?.let { msg ->
//                        call.respond(
//                            BadRequest,
//                            mapOf("error" to "invalid_mood", "message" to msg)
//                        )
//                        return@put
//                    }
//                }
                request.avatar?.let { avatar ->
                    Validation.validateAvatar(avatar)?.let { msg ->
                        call.respond(
                            BadRequest,
                            mapOf("error" to "invalid_avatar", "message" to msg)
                        )
                        return@put
                    }
                }

                val updated = userRepo.updateProfile(
                    userId = userId,
                    nickname = request.nickname,
                    mood = request.mood,
                    avatar = request.avatar
                )

                if (updated == null) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf("error" to "user_not_found")
                    )
                } else {
                    call.respond(updated.toProfile())
                }



            }

        }

        // Registration (no auth required)
        post("/api/users/register"){
            val request = try{
                call.receive<RegisterRequest>()
            } catch(e: Exception){
                print(e)
                call.respond(
                    BadRequest,
                    mapOf("error" to "bad_request", "message" to "Invalid body")
                )
                return@post
            }

            val user = userRepo.createUSer(
                nickname = request.nickname,
                mood = request.mood,
                avatar = request.avatar
            )

            val token  = generateToken(user.userId, jwtSettings)

            val response = RegisterResponse(
                userId = user.userId,
                authToken = token,
                profile = user.toProfile()
            )
            call.respond(response)
        }
    }
}

