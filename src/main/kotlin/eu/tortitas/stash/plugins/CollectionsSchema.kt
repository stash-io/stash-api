package eu.tortitas.stash.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*

@Serializable
data class ExposedCollection(val title: String, val description: String?, val published: Boolean, val userId: Int, val id: Int?)
class CollectionService(private val database: Database) {
    object Collections : Table() {
        val id = integer("id").autoIncrement()
        val title = varchar("title", length = 50)
        val description = text("description").nullable()
        val published = bool("published").default(false)
        val userId = (integer("user_id") references UserService.Users.id)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Collections)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(collection: ExposedCollection): Int = dbQuery {
        Collections.insert {
            it[title] = collection.title
            it[description] = collection.description
            it[published] = collection.published
            it[userId] = collection.userId
        }[Collections.id]
    }

    suspend fun read(id: Int, userId: Int): ExposedCollection? {
        return dbQuery {
            Collections.select { Collections.id eq id and (Collections.userId eq userId) }
                .map { ExposedCollection(
                    it[Collections.title], it[Collections.description], it[Collections.published], it[Collections.userId], it[Collections.id]
                ) }
                .singleOrNull()
        }
    }

    suspend fun readByUserId(userId: Int): List<ExposedCollection> {
        return dbQuery {
            Collections.select { Collections.userId eq userId }
                .map { ExposedCollection(it[Collections.title], it[Collections.description], it[Collections.published], it[Collections.userId], it[Collections.id]) }
                .toList()
        }
    }

    suspend fun update(id: Int, userId: Int, collection: ExposedCollection) {
        dbQuery {
            Collections.update({ Collections.id eq id and (Collections.userId eq userId) }) {
                it[title] = collection.title
                it[description] = collection.description
                it[published] = collection.published
            }
        }
    }

    suspend fun delete(id: Int, userId: Int) {
        dbQuery {
            Collections.deleteWhere { Collections.id.eq(id) and (Collections.userId eq userId) }
        }
    }
}

fun Application.provideCollectionService(): CollectionService {
    val database = getPostgresDatabase()
    return CollectionService(database)
}