package eu.tortitas.stash.plugins

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

@Serializable
data class ExposedLink(val title: String, val description: String?, val url: String?, val published: Boolean, val userId: Int, val createdAt: String?, val collectionId: Int?, val id: Int?)
class LinkService(private val database: Database) {
    object Links : Table() {
        val id = integer("id").autoIncrement()
        val title = varchar("title", length = 50)
        val description = text("description").nullable()
        val url = text("url").nullable()
        val published = bool("published").default(false)
        val userId = (integer("user_id") references UserService.Users.id)
        val collectionId = (integer("collection_id") references CollectionService.Collections.id).nullable()
        val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Links)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(link: ExposedLink): Int = dbQuery {
        Links.insert {
            it[title] = link.title
            it[description] = link.description
            it[url] = link.url
            it[published] = link.published
            it[userId] = link.userId
            it[collectionId] = link.collectionId
        }[Links.id]
    }

    suspend fun read(id: Int, userId: Int): ExposedLink? {
        return dbQuery {
            Links.select { Links.id eq id and (Links.userId eq userId) }
                .map { ExposedLink(
                    it[Links.title],
                    it[Links.description],
                    it[Links.url],
                    it[Links.published],
                    it[Links.userId],
                    it[Links.createdAt].toString(),
                    it[Links.collectionId],
                    it[Links.id],
                ) }
                .singleOrNull()
        }
    }

    suspend fun readByUserId(userId: Int): List<ExposedLink> {
        return dbQuery {
            Links.select { Links.userId eq userId }
                .map { ExposedLink(
                    it[Links.title],
                    it[Links.description],
                    it[Links.url],
                    it[Links.published],
                    it[Links.userId],
                    it[Links.createdAt].toString(),
                    it[Links.collectionId],
                    it[Links.id]
                ) }
                .toList()
        }
    }

    suspend fun readByUserIdBetweenDates(userId: Int, from: String, to: String): List<ExposedLink> {
        return dbQuery {
            Links.select { Links.userId eq userId and (Links.createdAt.between(from, to)) }
            .map { ExposedLink(
                it[Links.title],
                it[Links.description],
                it[Links.url],
                it[Links.published],
                it[Links.userId],
                it[Links.createdAt].toString(),
                it[Links.collectionId],
                it[Links.id]
            ) }
            .toList()
        }
    }

    suspend fun readByCollectionId(collectionId: Int): List<ExposedLink> {
        return dbQuery {
            Links.select { Links.collectionId eq collectionId }
                .map { ExposedLink(
                    it[Links.title],
                    it[Links.description],
                    it[Links.url],
                    it[Links.published],
                    it[Links.userId],
                    it[Links.createdAt].toString(),
                    it[Links.collectionId],
                    it[Links.id]
                ) }
                .toList()
        }
    }

    suspend fun update(id: Int, userId: Int, link: ExposedLink) {
        dbQuery {
            Links.update({ Links.id eq id and (Links.userId eq userId) }) {
                it[title] = link.title
                it[description] = link.description
                it[url] = link.url
                it[published] = link.published
                it[collectionId] = link.collectionId
            }
        }
    }

    suspend fun delete(id: Int, userId: Int) {
        dbQuery {
            Links.deleteWhere { Links.id.eq(id) and (Links.userId eq userId) }
        }
    }

    suspend fun deleteByUserId(userId: Int) {
        dbQuery {
            Links.deleteWhere { Links.userId.eq(userId) }
        }
    }
}

fun Application.provideLinkService(): LinkService {
    val database = getPostgresDatabase()
    return LinkService(database)
}