package com.aryanspatel.moodmatch.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Table

object Users: Table("users"){
    val userId = varchar("user_id", 50)
    val nickname = varchar("nickname", 50)
    val mood = varchar("mood", 20)
    val avatar = varchar("avatar", 20)
    val status = varchar("status", 20)
    val createdAt = long("created_at")
    val lastSeenAt = long("last_seen_at")

    override val primaryKey = PrimaryKey(userId)

}

object DatabaseFactory{
    fun init (config: ApplicationConfig){
        try {
        val dbConfig = config.config("db")

        val jdbcUrl = dbConfig.property("url").getString()
        val user = dbConfig.property("user").getString()
        val password = dbConfig.property("password").getString()
        val driver = dbConfig.property("driver").getString()

        val hikariConfig = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = user
            this.password = password
            this.driverClassName = driver
            maximumPoolSize = 5
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)

        // Run schema creation/migrations for v1 (idempotent)
        transaction {
            SchemaUtils.create(Users)
        }

        println("✅ Database initialized successfully: $jdbcUrl")
        } catch (e: Exception) {
            // Don’t crash the whole app; log clearly
            println("❌ Failed to initialize database: ${e.message}")
            e.printStackTrace()
        }
    }

    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}