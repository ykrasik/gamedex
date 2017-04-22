package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.*
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Blob
import javax.inject.Inject
import javax.inject.Singleton
import javax.sql.rowset.serial.SerialBlob

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 15:10
 */
interface PersistenceService {
    fun fetchAllLibraries(): List<Library>
    fun insertLibrary(path: File, data: LibraryData): Library
    fun deleteLibrary(id: Int)

    fun fetchAllGames(): List<RawGame>
    fun insertGame(metaData: MetaData, providerData: List<ProviderFetchResult>): RawGame
    fun deleteGame(id: Int)

    fun fetchImage(url: String): ByteArray?
    fun insertImage(gameId: Int, url: String, data: ByteArray): Unit
}

@Singleton
class PersistenceServiceImpl @Inject constructor(initializer: DbInitializer) : PersistenceService {
    init {
        initializer.create()
    }

    private val log by logger()

    override fun fetchAllLibraries(): List<Library> = transaction {
        log.info { "Fetching all libraries..." }
        val libraries = Libraries.selectAll().map {
            Library(
                id = it[Libraries.id].value,
                path = it[Libraries.path].toFile(),
                data = it[Libraries.data].fromJson()
            )
        }
        log.info { "Result: ${libraries.size} libraries." }
        libraries
    }

    override fun insertLibrary(path: File, data: LibraryData): Library = transaction {
        log.info { "Inserting library: path=$path, data=$data..." }
        val id = Libraries.insertAndGetId {
            it[Libraries.path] = path.toString()
            it[Libraries.data] = data.toJsonStr()
        }!!.value
        val library = Library(id, path, data)
        log.info { "Result: $library." }
        library
    }

    override fun deleteLibrary(id: Int) = transaction {
        log.debug { "Deleting Library($id)..." }
        val amount = Libraries.deleteWhere { Libraries.id.eq(id.toLibraryId()) }
        require(amount == 1) { "Doesn't exist: Library($id)" }
        log.debug { "Done." }
    }

    override fun fetchAllGames(): List<RawGame> = transaction {
        log.info { "Fetching all games..." }
        val games = Games.selectAll().map {
            RawGame(
                id = it[Games.id].value,
                metaData = MetaData(
                    path = it[Games.path].toFile(),
                    lastModified = it[Games.lastModified],
                    libraryId = it[Games.libraryId].value
                ),
                providerData = it[Games.data].listFromJson()
            )
        }
        log.info { "Result: ${games.size} games." }
        games
    }

    override fun insertGame(metaData: MetaData, providerData: List<ProviderFetchResult>): RawGame = transaction {
        log.info { "Inserting game: metaData=$metaData, providerData=$providerData..." }

        val id = Games.insertAndGetId {
            it[Games.libraryId] = metaData.libraryId.toLibraryId()
            it[Games.path] = metaData.path.path
            it[Games.lastModified] = metaData.lastModified
            it[Games.data] = providerData.toJsonStr()
        }!!.value

        val game = RawGame(id = id, metaData = metaData, providerData = providerData)
        log.info { "Result: $game." }
        game
    }

    override fun deleteGame(id: Int) = transaction {
        log.debug { "Deleting Game($id)..." }
        val amount = Games.deleteWhere { Games.id.eq(id.toGameId()) }
        require(amount == 1) { "Doesn't exist: Game($id)" }
        log.debug { "Done." }
    }

    override fun fetchImage(url: String): ByteArray? = transaction {
        Images.select { Images.url.eq(url) }.map {
            it[Images.bytes].bytes
        }.firstOrNull()
    }

    override fun insertImage(gameId: Int, url: String, data: ByteArray): Unit = transaction {
        Images.insert {
            it[Images.gameId] = gameId.toGameId()
            it[Images.url] = url
            it[Images.bytes] = data.toBlob()
        }
    }

    private fun Int.toLibraryId() = EntityID(this, Libraries)
    private fun Int.toGameId() = EntityID(this, Games)

    private val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())
    private fun ByteArray.toBlob(): Blob = SerialBlob(this)
}