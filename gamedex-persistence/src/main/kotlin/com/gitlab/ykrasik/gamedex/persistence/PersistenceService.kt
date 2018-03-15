package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.*
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
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
    fun dropDb()

    fun fetchLibraries(): List<Library>
    fun insertLibrary(path: File, data: LibraryData): Library
    fun updateLibrary(library: Library)
    fun deleteLibrary(id: Int)
    fun deleteLibraries(libraryIds: List<Int>): Int

    fun fetchGames(): List<RawGame>
    fun insertGame(metadata: Metadata, providerData: List<ProviderData>, userData: UserData?): RawGame
    fun updateGame(rawGame: RawGame): RawGame
    fun deleteGame(id: Int)
    fun deleteGames(gameIds: List<Int>): Int
    fun clearUserData(): Int

    fun fetchImage(url: String): ByteArray?
    fun insertImage(gameId: Int, url: String, data: ByteArray)
    fun fetchImagesExcept(except: List<String>): List<Pair<String, FileSize>>
    fun deleteImages(images: List<String>): Int
}

@Singleton
class PersistenceServiceImpl @Inject constructor(config: PersistenceConfig) : PersistenceService {
    private val log = logger()
    private val tables = arrayOf(Libraries, Games, Images)

    init {
        Database.connect(config.dbUrl, config.driver, config.user, config.password)

        create()
    }

    private fun create() = transaction {
        SchemaUtils.create(*tables)
    }

    private fun drop() = transaction {
        SchemaUtils.drop(*tables)
    }

    override fun dropDb() = transaction {
        drop()
        create()
    }

    override fun fetchLibraries(): List<Library> = transaction {
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
        }.value
        val library = Library(id, path, data)
        log.trace("Result: $library.")
        library
    }

    override fun updateLibrary(library: Library) = transaction {
        val rowsUpdated = Libraries.update(where = { Libraries.id.eq(library.id.toLibraryId()) }) {
            it[Libraries.path] = library.path.toString()
            it[Libraries.data] = library.data.toJsonStr()
        }
        require(rowsUpdated == 1) { "Doesn't exist: Library(${library.id})!" }
    }

    override fun deleteLibrary(id: Int) = transaction {
        log.trace("Deleting Library($id)...")
        val amount = Libraries.deleteWhere { Libraries.id.eq(id.toLibraryId()) }
        require(amount == 1) { "Doesn't exist: Library($id)" }
        log.trace("Done.")
    }

    override fun deleteLibraries(libraryIds: List<Int>): Int = transaction {
        Libraries.deleteWhere { Libraries.id.inList(libraryIds) }
    }

    override fun fetchGames(): List<RawGame> = transaction {
        log.trace("Fetching all games...")
        val games = Games.selectAll().map {
            RawGame(
                id = it[Games.id].value,
                metadata = Metadata(
                    path = it[Games.path],
                    updateDate = it[Games.updateDate].withZone(DateTimeZone.UTC),
                    libraryId = it[Games.libraryId].value
                ),
                providerData = it[Games.providerData].listFromJson(),
                userData = it[Games.userData]?.fromJson()
            )
        }
        log.trace("Result: ${games.size} games.")
        games
    }

    override fun insertGame(metadata: Metadata, providerData: List<ProviderData>, userData: UserData?): RawGame = transaction {
        log.trace("Inserting game: metadata=$metadata, rawGameData=$providerData...")

        val updateDate = now

        val id = Games.insertAndGetId {
            it[Games.libraryId] = metadata.libraryId.toLibraryId()
            it[Games.path] = metadata.path
            it[Games.updateDate] = updateDate
            it[Games.providerData] = providerData.toJsonStr()
            it[Games.userData] = userData?.toJsonStr()
        }.value

        val game = RawGame(id = id, metadata = metadata.updated(updateDate), providerData = providerData, userData = userData)
        log.trace("Result: $game.")
        game
    }

    override fun updateGame(rawGame: RawGame): RawGame = transaction {
        val updateDate = now

        val rowsUpdated = Games.update(where = { Games.id.eq(rawGame.id.toGameId()) }) {
            it[Games.libraryId] = rawGame.metadata.libraryId.toLibraryId()
            it[Games.path] = rawGame.metadata.path
            it[Games.updateDate] = updateDate
            it[Games.providerData] = rawGame.providerData.toJsonStr()
            it[Games.userData] = rawGame.userData?.toJsonStr()
        }
        require(rowsUpdated == 1) { "Doesn't exist: Game(${rawGame.id})!" }

        rawGame.updated(updateDate)
    }

    override fun deleteGame(id: Int) = transaction {
        log.trace("Deleting Game($id)...")
        val rowsDeleted = Games.deleteWhere { Games.id.eq(id.toGameId()) }
        require(rowsDeleted == 1) { "Doesn't exist: Game($id)!" }
        log.trace("Done.")
    }

    override fun deleteGames(gameIds: List<Int>): Int = transaction {
        Games.deleteWhere { Games.id.inList(gameIds) }
    }

    override fun clearUserData() = transaction {
        Games.updateAll {
            it[Games.userData] = null
        }
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

    override fun fetchImagesExcept(except: List<String>): List<Pair<String, FileSize>> = transaction {
        Images.select { Images.url.notInList(except) }.map {
            it[Images.url] to FileSize(it[Images.bytes].length())
        }
    }

    override fun deleteImages(images: List<String>) = transaction {
        Images.deleteWhere { Images.url.inList(images) }
    }

    private fun RawGame.updated(updateDate: DateTime) = withMetadata { it.updated(updateDate) }
    private fun Metadata.updated(updateDate: DateTime) = copy(updateDate = updateDate)

    private fun Int.toLibraryId() = EntityID(this, Libraries)
    private fun Int.toGameId() = EntityID(this, Games)

    private val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())
    private fun ByteArray.toBlob(): Blob = SerialBlob(this)
}