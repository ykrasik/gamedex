package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.datamodel.*
import com.github.ykrasik.gamedex.common.util.fromJson
import com.github.ykrasik.gamedex.common.util.logger
import com.github.ykrasik.gamedex.common.util.toFile
import com.github.ykrasik.gamedex.common.util.toJsonStr
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
// FIXME: There is no need for individual DAO objects. It's ok for namespace segregation, but the whole code can be contained in this class.
interface PersistenceService {
    fun fetchAllLibraries(): List<Library>
    fun insert(request: AddLibraryRequest): Library
    fun deleteLibrary(id: Int)

    fun fetchAllGames(): List<Game>
    fun insert(request: AddGameRequest): Game
    fun deleteGame(id: Int)

    fun fetchImage(id: GameImageId): GameImage
    fun updateImage(image: GameImage): Unit
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)

data class AddGameRequest(
    val metaData: MetaData,
    val gameData: GameData,
    val providerData: List<ProviderData>,
    val imageData: ImageData
)

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

    override fun insert(request: AddLibraryRequest): Library = transaction {
        log.info { "Inserting: $request..." }
        val id = Libraries.insertAndGetId {
            it[path] = request.path.toString()
            it[data] = request.data.toJsonStr()
        }!!.value
        val library = Library(id, request.path, request.data)
        log.info { "Result: $library." }
        library
    }

    override fun deleteLibrary(id: Int) = transaction {
        log.debug { "Deleting Library($id)..." }
        val amount = Libraries.deleteWhere { Libraries.id.eq(id.toLibraryId()) }
        require(amount == 1) { "Doesn't exist: Library($id)" }
        log.debug { "Done." }
    }

    override fun fetchAllGames(): List<Game> = transaction {
        log.info { "Fetching all games..." }
        val games = Games.selectAll().map {
            val serializedData: SerializedGameData = it[Games.data].fromJson()
            Game(
                id = it[Games.id].value,
                metaData = MetaData(
                    path = it[Games.path].toFile(),
                    lastModified = it[Games.lastModified],
                    libraryId = it[Games.libraryId].value
                ),
                providerData = serializedData.providerData,
                gameData = serializedData.gameData
            )
        }
        log.info { "Result: ${games.size} games." }
        games
    }

    override fun insert(request: AddGameRequest): Game = transaction {
        log.info { "Inserting: $request..." }

        val id = insertGame(request.metaData, request.gameData, request.providerData)
        insertImage(id, request.imageData)

        val game = Game(id, request.metaData, request.gameData, request.providerData)
        log.info { "Result: $game." }
        game
    }

    private fun insertGame(metaData: MetaData, gameData: GameData, providerData: List<ProviderData>): Int = Games.insertAndGetId {
        it[Games.libraryId] = metaData.libraryId.toLibraryId()
        it[Games.path] = metaData.path.path
        it[Games.lastModified] = metaData.lastModified
        it[Games.data] = SerializedGameData(gameData, providerData).toJsonStr()
    }!!.value

    private fun insertImage(id: Int, imageData: ImageData) = Images.insert {
        it[gameId] = id.toGameId()
        it[thumbnailUrl] = imageData.thumbnailUrl
        it[posterUrl] = imageData.posterUrl
        it[screenshot1Url] = imageData.screenshot1Url
        it[screenshot2Url] = imageData.screenshot2Url
        it[screenshot3Url] = imageData.screenshot3Url
        it[screenshot4Url] = imageData.screenshot4Url
        it[screenshot5Url] = imageData.screenshot5Url
        it[screenshot6Url] = imageData.screenshot6Url
        it[screenshot7Url] = imageData.screenshot7Url
        it[screenshot8Url] = imageData.screenshot8Url
        it[screenshot9Url] = imageData.screenshot9Url
        it[screenshot10Url] = imageData.screenshot10Url
    }

    override fun deleteGame(id: Int) = transaction {
        log.debug { "Deleting Game($id)..." }
        val amount = Games.deleteWhere { Games.id.eq(id.toGameId()) }
        require(amount == 1) { "Doesn't exist: Game($id)" }
        log.debug { "Done." }
    }

    override fun fetchImage(id: GameImageId): GameImage = transaction {
        log.debug { "Fetching: $id..." }
        val (dataColumn, urlColumn) = id.toColumns()
        val data = fetchImage(id, dataColumn, urlColumn)
        log.debug { "Result: $data." }
        data
    }

    private fun fetchImage(id: GameImageId, dataColumn: Column<Blob?>, urlColumn: Column<String?>): GameImage {
        val result = Images.slice(dataColumn, urlColumn).select { Images.gameId.eq(id.gameId.toGameId()) }.firstOrNull() ?:
            throw IllegalArgumentException("Game(${id.gameId} doesn't exist!")
        return GameImage(
            id = id,
            bytes = result[dataColumn]?.bytes,
            url = result[urlColumn]
        )
    }

    override fun updateImage(image: GameImage) = transaction {
        log.debug { "Updating: $image..." }
        val (dataColumn, urlColumn) = image.id.toColumns()
        updateImage(image, dataColumn, urlColumn)
        log.debug { "Done." }
    }

    private fun updateImage(image: GameImage, dataColumn: Column<Blob?>, urlColumn: Column<String?>) {
        val count = Images.update(where = { Images.gameId.eq(image.id.gameId.toGameId()) }) {
            it[dataColumn] = image.bytes?.toBlob()
            it[urlColumn] = image.url
        }
        check(count == 1) { "Updated invalid amount of rows ($count) when updating image for $image!" }
    }

    private fun GameImageId.toColumns(): Pair<Column<Blob?>, Column<String?>> = when (this.type) {
        GameImageType.Thumbnail -> Pair(Images.thumbnail, Images.thumbnailUrl)
        GameImageType.Poster -> Pair(Images.poster, Images.posterUrl)
        GameImageType.Screenshot1 -> Pair(Images.screenshot1, Images.screenshot1Url)
        GameImageType.Screenshot2 -> Pair(Images.screenshot2, Images.screenshot2Url)
        GameImageType.Screenshot3 -> Pair(Images.screenshot3, Images.screenshot3Url)
        GameImageType.Screenshot4 -> Pair(Images.screenshot4, Images.screenshot4Url)
        GameImageType.Screenshot5 -> Pair(Images.screenshot5, Images.screenshot5Url)
        GameImageType.Screenshot6 -> Pair(Images.screenshot6, Images.screenshot6Url)
        GameImageType.Screenshot7 -> Pair(Images.screenshot7, Images.screenshot7Url)
        GameImageType.Screenshot8 -> Pair(Images.screenshot8, Images.screenshot8Url)
        GameImageType.Screenshot9 -> Pair(Images.screenshot9, Images.screenshot9Url)
        GameImageType.Screenshot10 -> Pair(Images.screenshot10, Images.screenshot10Url)
        else -> throw IllegalArgumentException("Invalid gameImageType: ${this.type}!")
    }

    private data class SerializedGameData(
        val gameData: GameData,
        val providerData: List<ProviderData>
    )

    private fun Int.toLibraryId() = EntityID(this, Libraries)
    private fun Int.toGameId() = EntityID(this, Games)

    // TODO: don't use this, stream the blob & report progress.
    private val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())

    private fun ByteArray.toBlob(): Blob = SerialBlob(this)
}