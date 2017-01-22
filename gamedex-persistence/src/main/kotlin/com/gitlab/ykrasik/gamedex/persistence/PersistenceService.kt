package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.common.jackson.fromJson
import com.github.ykrasik.gamedex.common.jackson.toJsonStr
import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.common.toFile
import com.github.ykrasik.gamedex.datamodel.*
import com.gitlab.ykrasik.gamedex.persistence.entity.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
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
    fun insert(request: AddLibraryRequest): Int
    fun deleteLibrary(id: Int)

    fun fetchAllGames(): List<Game>
    fun insert(request: AddGameRequest): Int
    fun deleteGame(id: Int)

    fun fetchImage(id: GameImageId): GameImage
    fun updateImage(image: GameImage): Unit
}

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)

data class AddGameRequest(
    val libraryId: Int,
    val path: File,
    val lastModified: DateTime,
    val gameData: GameData,
    val imageData: GameImageData
)

@Singleton
class PersistenceServiceImpl @Inject constructor(initializer: DbInitializer) : PersistenceService {
    init {
        initializer.init()
    }

    private val log by logger()

    override fun fetchAllLibraries(): List<Library> = transaction {
        log.debug { "Fetching all libraries..." }
        val libraries = Libraries.selectAll().map {
            Library(
                id = it[Libraries.id],
                path = it[Libraries.path].toFile(),
                data = it[Libraries.data].fromJson()
            )
        }
        log.debug { "Result: ${libraries.size} libraries." }
        libraries
    }

    override fun insert(request: AddLibraryRequest): Int = transaction {
        log.debug { "Inserting: $request..." }
        val id = Libraries.insert {
            it[path] = request.path.toString()
            it[data] = request.data.toJsonStr()
        } get Libraries.id
        log.debug { "Result: Library($id)." }
        id
    }

    override fun deleteLibrary(id: Int) = transaction {
        log.debug { "Deleting Library($id)..." }
        val amount = Libraries.deleteWhere { Libraries.id.eq(id) }
        require(amount == 1) { "Doesn't exist: Library($id)" }
        log.debug { "Done." }
    }

    override fun fetchAllGames(): List<Game> = transaction {
        log.debug { "Fetching all games..." }
        val games = Games.selectAll().map {
            Game(
                id = it[Games.id],
                path = it[Games.path].toFile(),
                lastModified = it[Games.lastModified],
                libraryId = it[Games.libraryId],
                data = it[Games.data].fromJson()
            )
        }
        log.debug { "Result: ${games.size} games." }
        games
    }

    override fun insert(request: AddGameRequest): Int = transaction {
        log.debug { "Inserting: $request..." }

        val id = insertGame(request)
        insertImage(id, request.imageData)

        log.debug { "Result: Game($id)." }
        id
    }

    private fun insertGame(request: AddGameRequest): Int = Games.insert {
        it[Games.libraryId] = request.libraryId
        it[Games.path] = request.path.path
        it[Games.lastModified] = request.lastModified
        it[Games.data] = request.gameData.toJsonStr()
    } get Games.id

    private fun insertImage(id: Int, imageData: GameImageData) = Images.insert {
        it[gameId] = id
        it[thumbnailUrl] = imageData.thumbnailUrl
        it[posterUrl] = imageData.posterUrl
        // FIXME: Add screenshots
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