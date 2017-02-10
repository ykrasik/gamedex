package com.gitlab.ykrasik.gamedex.persistence

import com.gitlab.ykrasik.gamedex.common.datamodel.*
import com.gitlab.ykrasik.gamedex.common.util.fromJson
import com.gitlab.ykrasik.gamedex.common.util.logger
import com.gitlab.ykrasik.gamedex.common.util.toFile
import com.gitlab.ykrasik.gamedex.common.util.toJsonStr
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
    fun insert(request: AddLibraryRequest): Library
    fun deleteLibrary(id: Int)

    fun fetchAllGames(): List<Game>
    fun insert(request: AddGameRequest): Game
    fun deleteGame(id: Int)

    fun fetchImage(id: Int): GameImage
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
    val imageUrls: ImageUrls
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
            val gameId = it[Games.id].value
            val imageIds = fetchImageIds(gameId)
            val serializedData: SerializedGameData = it[Games.data].fromJson()
            Game(
                id = gameId,
                metaData = MetaData(
                    path = it[Games.path].toFile(),
                    lastModified = it[Games.lastModified],
                    libraryId = it[Games.libraryId].value
                ),
                gameData = serializedData.gameData,
                providerData = serializedData.providerData,
                imageIds = imageIds
            )
        }
        log.info { "Result: ${games.size} games." }
        games
    }

    private fun fetchImageIds(gameId: Int): ImageIds {
        val images = Images.slice(Images.id, Images.type).select { Images.gameId.eq(gameId.toGameId()) }.map {
            it[Images.type] to it[Images.id]
        }.groupBy(keySelector = { it.first }, valueTransform = { it.second.value })

        return ImageIds(
            thumbnailId = images[GameImageType.Thumbnail]?.first(),
            posterId = images[GameImageType.Poster]?.first(),
            screenshotIds = images[GameImageType.Screenshot] ?: emptyList()
        )
    }

    override fun insert(request: AddGameRequest): Game = transaction {
        log.info { "Inserting: $request..." }

        val id = insertGame(request.metaData, request.gameData, request.providerData)
        val imageIds = insertImages(id, request.imageUrls)

        val game = Game(
            id = id,
            metaData = request.metaData,
            gameData = request.gameData,
            providerData = request.providerData,
            imageIds = imageIds
        )
        log.info { "Result: $game." }
        game
    }

    private fun insertGame(metaData: MetaData, gameData: GameData, providerData: List<ProviderData>): Int = Games.insertAndGetId {
        it[Games.libraryId] = metaData.libraryId.toLibraryId()
        it[Games.path] = metaData.path.path
        it[Games.lastModified] = metaData.lastModified
        it[Games.data] = SerializedGameData(gameData, providerData).toJsonStr()
    }!!.value

    private fun insertImages(gameId: Int, imageUrls: ImageUrls): ImageIds {
        val thumbnailId = insertImage(gameId, GameImageType.Thumbnail, imageUrls.thumbnailUrl)
        val posterId = insertImage(gameId, GameImageType.Poster, imageUrls.posterUrl)
        val screenshotIds = imageUrls.screenshotUrls.mapNotNull { insertImage(gameId, GameImageType.Screenshot, it) }
        return ImageIds(thumbnailId, posterId, screenshotIds)
    }

    private fun insertImage(gameId: Int, type: GameImageType, url: String?): Int? {
        if (url == null) return null
        return Images.insertAndGetId {
            it[Images.gameId] = gameId.toGameId()
            it[Images.type] = type
            it[Images.url] = url
        }!!.value
    }

    override fun deleteGame(id: Int) = transaction {
        log.debug { "Deleting Game($id)..." }
        val amount = Games.deleteWhere { Games.id.eq(id.toGameId()) }
        require(amount == 1) { "Doesn't exist: Game($id)" }
        log.debug { "Done." }
    }

    override fun fetchImage(id: Int): GameImage = transaction {
        Images.select { Images.id.eq(id.toImageId()) }.map {
            GameImage(
                id = it[Images.id].value,
                url = it[Images.url],
                bytes = it[Images.bytes]?.bytes
            )
        }.first()
    }

    override fun updateImage(image: GameImage) = transaction {
        log.debug { "Updating: $image..." }
        val count = Images.update(where = { Images.id.eq(image.id.toImageId()) }) {
            it[Images.url] = image.url
            it[Images.bytes] = image.bytes?.toBlob()
        }
        check(count == 1) { "Updated invalid amount of rows ($count) when updating image for $image!" }
        log.debug { "Done." }
    }

    private fun Int.toLibraryId() = EntityID(this, Libraries)
    private fun Int.toGameId() = EntityID(this, Games)
    private fun Int.toImageId() = EntityID(this, Images)

    // TODO: stream the blob & report progress instead?
    private val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())
    private fun ByteArray.toBlob(): Blob = SerialBlob(this)

    private data class SerializedGameData(
        val gameData: GameData,
        val providerData: List<ProviderData>
    )
}