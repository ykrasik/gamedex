package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.*
import com.github.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.sql.Blob
import javax.inject.Inject
import javax.inject.Singleton

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

    // TODO: Should this return a Game, and handle all the sort stuff internally?
    fun fetchAllGames(): List<RawGame>
    fun insert(request: AddGameRequest): RawGame
    fun deleteGame(id: Int)

    fun fetchImage(id: GameImageId): GameImage
    fun updateImage(image: GameImage): Unit
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)

data class AddGameRequest(
    val metaData: GameMetaData,
    val providerData: List<ProviderGameData>,
    val imageData: GameImageData
)

data class RawGame(
    val id: Int,
    val metaData: GameMetaData,
    val providerData: List<ProviderGameData>
) {
    override fun toString() = "RawGame(id = $id, path = ${metaData.path})"
}

@Singleton
class PersistenceServiceImpl @Inject constructor(initializer: DbInitializer) : PersistenceService {
    init {
        initializer.init()
    }

    private val log by logger()

    override fun fetchAllLibraries(): List<Library> = transaction {
        log.info { "Fetching all libraries..." }
        val libraries = Libraries.selectAll().map {
            Library(
                id = it[Libraries.id],
                path = it[Libraries.path].toFile(),
                data = it[Libraries.data].fromJson()
            )
        }
        log.info { "Result: ${libraries.size} libraries." }
        libraries
    }

    override fun insert(request: AddLibraryRequest): Library = transaction {
        log.info { "Inserting: $request..." }
        val id = Libraries.insert {
            it[path] = request.path.toString()
            it[data] = request.data.toJsonStr()
        } get Libraries.id
        val library = Library(id, request.path, request.data)
        log.info { "Result: $library." }
        library
    }

    override fun deleteLibrary(id: Int) = transaction {
        log.debug { "Deleting Library($id)..." }
        val amount = Libraries.deleteWhere { Libraries.id.eq(id) }
        require(amount == 1) { "Doesn't exist: Library($id)" }
        log.debug { "Done." }
    }

    override fun fetchAllGames(): List<RawGame> = transaction {
        log.info { "Fetching all games..." }
        val games = Games.selectAll().map {
            RawGame(
                id = it[Games.id],
                metaData = GameMetaData(
                    path = it[Games.path].toFile(),
                    lastModified = it[Games.lastModified],
                    libraryId = it[Games.libraryId]
                ),
                providerData = it[Games.data].listFromJson()
            )
        }
        log.info { "Result: ${games.size} games." }
        games
    }

    override fun insert(request: AddGameRequest): RawGame = transaction {
        log.info { "Inserting: $request..." }

        val id = insertGame(request.metaData, request.providerData)
        insertImage(id, request.imageData)

        val game = RawGame(id, request.metaData, request.providerData)
        log.info { "Result: $game." }
        game
    }

    private fun insertGame(metaData: GameMetaData, providerData: List<ProviderGameData>): Int = Games.insert {
        it[Games.libraryId] = metaData.libraryId
        it[Games.path] = metaData.path.path
        it[Games.lastModified] = metaData.lastModified
        it[Games.data] = providerData.toJsonStr()
    } get Games.id

    private fun insertImage(id: Int, imageData: GameImageData) = Images.insert {
        it[gameId] = id
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
        val amount = Games.deleteWhere { Games.id.eq(id) }
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
        val result = Images.slice(dataColumn, urlColumn).select { Images.gameId.eq(id.gameId) }.first()
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
        val count = Images.update(where = { Images.gameId.eq(image.id.gameId) }) {
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
}