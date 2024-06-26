package eu.tortitas.stash.plugins

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*

@Serializable
data class ExposedUser(val username: String, val email: String, val password: String, val role: String, val reminderDayOfWeek: String?, val stripeCustomerId: String?, val id: Int?)
class UserService(private val database: Database) {
    object Users : Table() {
        val id = integer("id").autoIncrement()
        val username = varchar("username", length = 50)
        val email = varchar("email", length = 50)
        val password = text("password")
        val role = varchar("role", length = 50) // 'tier1' | 'tier2' | 'tier3' | 'admin'
        val reminderDayOfWeek = varchar("reminderDayOfWeek", length = 50).nullable() // from 1 (Monday) to 7 (Sunday)
        val stripeCustomerId = text("stripe_customer_id").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Users)
            SchemaUtils.createMissingTablesAndColumns(Users)

            if (Users.select { Users.role eq "admin" }.count().toInt() == 0) {
                Users.insert {
                    it[username] = "admin"
                    it[email] = "admin@example.com"
                    it[password] = BCrypt.withDefaults().hashToString(12, "secret".toCharArray())
                    it[role] = "admin"
                    it[reminderDayOfWeek] = null
                    it[stripeCustomerId] = null
                }[Users.id]
            }
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(user: ExposedUser): Int = dbQuery {
        Users.insert {
            it[username] = user.username
            it[email] = user.email
            it[password] = user.password
            it[role] = user.role
            it[reminderDayOfWeek] = user.reminderDayOfWeek
            it[stripeCustomerId] = user.stripeCustomerId
        }[Users.id]
    }

    suspend fun read(id: Int): ExposedUser? {
        return dbQuery {
            Users.select { Users.id eq id }
                .map { ExposedUser(
                    it[Users.username],
                    it[Users.email],
                    it[Users.password],
                    it[Users.role],
                    it[Users.reminderDayOfWeek],
                    it[Users.stripeCustomerId],
                    it[Users.id])
                }
                .singleOrNull()
        }
    }

    suspend fun readByEmail(email: String): ExposedUser? {
        return dbQuery {
            Users.select { Users.email eq email }
                .map { ExposedUser(
                    it[Users.username],
                    it[Users.email],
                    it[Users.password],
                    it[Users.role],
                    it[Users.reminderDayOfWeek],
                    it[Users.stripeCustomerId],
                    it[Users.id])
                }
                .singleOrNull()
        }
    }

    suspend fun readAll(): List<ExposedUser> {
        return dbQuery {
            Users.selectAll().map { ExposedUser(
                it[Users.username],
                it[Users.email],
                it[Users.password],
                it[Users.role],
                it[Users.reminderDayOfWeek],
                it[Users.stripeCustomerId],
                it[Users.id])
                }
        }
    }

    suspend fun update(id: Int, user: ExposedUser) {
        dbQuery {
            Users.update({ Users.id eq id }) {
                it[username] = user.username
                it[email] = user.email
                it[password] = user.password
                it[role] = user.role
                it[reminderDayOfWeek] = user.reminderDayOfWeek
                it[stripeCustomerId] = user.stripeCustomerId
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Users.deleteWhere { Users.id.eq(id) }
        }
    }
}

fun Application.provideUserService(): UserService {
    val database = getPostgresDatabase()
    return UserService(database)
}