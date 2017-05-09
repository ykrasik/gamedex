package com.gitlab.ykrasik.gamedex.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.core.GamedexTask
import com.gitlab.ykrasik.gamedex.repository.AddGameRequest
import com.gitlab.ykrasik.gamedex.repository.AddLibraryRequest
import com.gitlab.ykrasik.gamedex.repository.GameRepository
import com.gitlab.ykrasik.gamedex.repository.LibraryRepository
import com.gitlab.ykrasik.gamedex.util.toFile
import org.joda.time.DateTime
import org.joda.time.LocalDate
import tornadofx.Controller
import tornadofx.chooseFile
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.CoroutineContext

/**
 * User: ykrasik
 * Date: 09/05/2017
 * Time: 17:09
 */
@Singleton
class SettingsController @Inject constructor(
    private val gameRepository: GameRepository,
    private val libraryRepository: LibraryRepository
) : Controller() {

    private val objectMapper = ObjectMapper()
        .registerModule(KotlinModule())
        .registerModule(JodaModule())
        .configure(WRITE_DATES_AS_TIMESTAMPS, true)

    fun exportDatabase() {
        val file = browse() ?: return
        ExportDatabaseTask(file).start()
    }

    fun importDatabase() {
        // TODO: Delete existing db before.
        val file = browse() ?: return
        ImportDatabaseTask(file).start()
    }

    private fun browse(): File? {
        val file = chooseFile("Choose database file...", filters = emptyArray())
        return file.firstOrNull()
    }

    // TODO: This isn't actually cancellable.
    inner class ExportDatabaseTask(private val file: File) : GamedexTask("Exporting Database...") {
        suspend override fun doRun(context: CoroutineContext) {
            val libraries = libraryRepository.libraries.map { it.serialize() }
            val games = gameRepository.games.mapIndexed {i, game ->
                progress.progress(i, gameRepository.games.size - 1)
                game.serialize()
            }

            progress.message = "Writing file..."
            val portableDb = PortableDb(libraries, games)
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, portableDb)
        }

        override fun finally() {
            progress.message = "Done: Exported ${gameRepository.games.size} games."
        }
    }

    // TODO: This isn't actually cancellable.
    inner class ImportDatabaseTask(private val file: File) : GamedexTask("Importing Database...") {
        private var importedGames = 0

        suspend override fun doRun(context: CoroutineContext) {
            val portableDb = objectMapper.readValue(file, PortableDb::class.java)

            val libraries = portableDb.libraries
                .map { it.deserializeLibrary() }
                .map { libraryRepository.add(it) }
                .associateBy { it.path.toString() to it.platform }

            val requests = portableDb.games.map { game -> game.deserializeGame(libraries) }

            progress.message = "Writing DB..."
            gameRepository.addAll(requests, progress)
            importedGames = requests.size
        }

        override fun finally() {
            progress.message = "Done: Imported $importedGames games."
        }
    }

    private data class PortableDb(
        val libraries: List<Map<String, Any?>>,
        val games: List<Map<String, Any?>>
    )

    private fun Library.serialize() = filterNulls(
        "path" to path.toString(),
        "platform" to platform.toString(),
        "name" to name
    )

    private fun Map<String, Any?>.deserializeLibrary() = AddLibraryRequest(
        path = get_("path").toFile(),
        data = LibraryData(
            platform = Platform(get_("platform")),
            name = get_("name")
        )
    )

    private fun Game.serialize() = metaData.serialize(this) + filterNulls(
        "providerData" to rawGame.rawGameData.map { it.serialize() },
        "userData" to userData
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.deserializeGame(libraries: Map<Pair<String, Platform>, Library>) = AddGameRequest(
        metaData = deserializeMetaData(libraries),
        rawGameData = (get("providerData")!! as List<Map<String, Any?>>).apply { println(this) }.map { it.deserializeRawGameData() },
        userData = get("userData")?.let { userData ->
            objectMapper.readValue(objectMapper.writeValueAsString(userData), UserData::class.java)
        }
    )

    private fun MetaData.serialize(game: Game) = filterNulls(
        "path" to path.toString(),
        "lastModified" to lastModified,
        "libraryPath" to game.library.path,
        "platform" to game.platform.toString()
    )

    private fun Map<String, Any?>.deserializeMetaData(libraries: Map<Pair<String, Platform>, Library>): MetaData {
        val path = get_("libraryPath")
        val platform = Platform(get_("platform"))
        return MetaData(
            libraryId = libraries[path to platform]!!.id,
            path = get_("path").toFile(),
            lastModified = DateTime(get("lastModified")!! as Long)
        )
    }

    private fun RawGameData.serialize() = providerData.serialize() + gameData.serialize() + imageUrls.serialize()

    private fun Map<String, Any?>.deserializeRawGameData() = RawGameData(
        providerData = deserializeProviderData(),
        gameData = deserializeGameData(),
        imageUrls = deserializeImageUrls()
    )

    private fun ProviderData.serialize() = filterNulls(
        "provider" to type.toString(),
        "apiUrl" to apiUrl,
        "siteUrl" to siteUrl
    )

    private fun Map<String, Any?>.deserializeProviderData() = ProviderData(
        type = GameProviderType.valueOf(get_("provider")),
        apiUrl = get_("apiUrl"),
        siteUrl = get_("siteUrl")
    )

    private fun GameData.serialize() = filterNulls(
        "name" to name,
        "description" to description,
        "releaseDate" to releaseDate?.toDateTimeAtStartOfDay(),
        "criticScore" to criticScore,
        "userScore" to userScore,
        "genres" to genres
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.deserializeGameData() = GameData(
        name = get_("name"),
        description = get("description")?.toString(),
        releaseDate = (get("releaseDate") as? Long)?.let(::LocalDate),
        criticScore = get("criticScore") as? Double,
        userScore = get("userScore") as? Double,
        genres = get("genres") as List<String>
    )

    private fun ImageUrls.serialize() = filterNulls(
        "thumbnailUrl" to thumbnailUrl,
        "posterUrl" to posterUrl,
        "screenshotUrls" to screenshotUrls
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.deserializeImageUrls() = ImageUrls(
        thumbnailUrl = get("thumbnailUrl") as? String,
        posterUrl = get("posterUrl") as? String,
        screenshotUrls = get("screenshotUrls") as List<String>
    )

    private fun filterNulls(vararg values: Pair<String, Any?>) = values.filter { it.second != null }.toMap()

    private fun Map<String, Any?>.get_(key: String) = get(key)!!.toString()
}