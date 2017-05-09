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
    fun insertGame(metaData: MetaData, rawGameData: List<RawGameData>): RawGame
    fun updateGame(rawGame: RawGame): Unit
    fun deleteGame(id: Int)

    fun fetchImage(url: String): ByteArray?
    fun insertImage(gameId: Int, url: String, data: ByteArray): Unit
}

@Singleton
class PersistenceServiceImpl @Inject constructor(initializer: DbInitializer) : PersistenceService {
    private val log = logger()

    init {
        initializer.create()
    }

    override fun fetchAllLibraries(): List<Library> = transaction {
        log.trace("Fetching all libraries...")
        val libraries = Libraries.selectAll().map {
            Library(
                id = it[Libraries.id].value,
                path = it[Libraries.path].toFile(),
                data = it[Libraries.data].fromJson()
            )
        }
        log.trace("Result: ${libraries.size} libraries.")
        libraries
    }

    override fun insertLibrary(path: File, data: LibraryData): Library = transaction {
        log.trace("Inserting library: path=$path, data=$data...")
        val id = Libraries.insertAndGetId {
            it[Libraries.path] = path.toString()
            it[Libraries.data] = data.toJsonStr()
        }!!.value
        val library = Library(id, path, data)
        log.trace("Result: $library.")
        library
    }

    override fun deleteLibrary(id: Int) = transaction {
        log.trace("Deleting Library($id)...")
        val amount = Libraries.deleteWhere { Libraries.id.eq(id.toLibraryId()) }
        require(amount == 1) { "Doesn't exist: Library($id)" }
        log.trace("Done.")
    }

    override fun fetchAllGames(): List<RawGame> = transaction {
        log.trace("Fetching all games...")
        val games = Games.selectAll().map {
            RawGame(
                id = it[Games.id].value,
                metaData = MetaData(
                    path = it[Games.path].toFile(),
                    lastModified = it[Games.lastModified],
                    libraryId = it[Games.libraryId].value
                ),
                rawGameData = it[Games.providerData].listFromJson(),
                userData = it[Games.userData]?.fromJson()
            )
        }
        log.trace("Result: ${games.size} games.")
        games
    }

    override fun insertGame(metaData: MetaData, rawGameData: List<RawGameData>): RawGame = transaction {
        log.trace("Inserting game: metaData=$metaData, rawGameData=$rawGameData...")

        val id = Games.insertAndGetId {
            it[Games.libraryId] = metaData.libraryId.toLibraryId()
            it[Games.path] = metaData.path.path
            it[Games.lastModified] = metaData.lastModified
            it[Games.providerData] = rawGameData.toJsonStr()
        }!!.value

        val game = RawGame(id = id, metaData = metaData, rawGameData = rawGameData, userData = null)
        log.trace("Result: $game.")
        game
    }

    override fun updateGame(rawGame: RawGame) = transaction {
        val rowsUpdated = Games.update(where = { Games.id.eq(rawGame.id.toGameId()) }) {
            it[Games.libraryId] = rawGame.metaData.libraryId.toLibraryId()
            it[Games.path] = rawGame.metaData.path.path
            it[Games.lastModified] = rawGame.metaData.lastModified
            it[Games.providerData] = rawGame.rawGameData.toJsonStr()
            it[Games.userData] = rawGame.userData?.toJsonStr()
        }
        require(rowsUpdated == 1) { "Doesn't exist: Game(${rawGame.id})!"}
    }

    override fun deleteGame(id: Int) = transaction {
        log.trace("Deleting Game($id)...")
        val rowsDeleted = Games.deleteWhere { Games.id.eq(id.toGameId()) }
        require(rowsDeleted == 1) { "Doesn't exist: Game($id)!" }
        log.trace("Done.")
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