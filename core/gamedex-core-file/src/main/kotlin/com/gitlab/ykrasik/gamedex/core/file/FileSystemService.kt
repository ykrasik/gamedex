/****************************************************************************
 * Copyright (C) 2016-2020 Yevgeny Krasik                                   *
 *                                                                          *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 *                                                                          *
 * http://www.apache.org/licenses/LICENSE-2.0                               *
 *                                                                          *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 ****************************************************************************/

package com.gitlab.ykrasik.gamedex.core.file

import com.gitlab.ykrasik.gamedex.FileTree
import com.gitlab.ykrasik.gamedex.Game
import com.gitlab.ykrasik.gamedex.GameId
import com.gitlab.ykrasik.gamedex.core.EventBus
import com.gitlab.ykrasik.gamedex.core.flowOf
import com.gitlab.ykrasik.gamedex.core.game.GameEvent
import com.gitlab.ykrasik.gamedex.core.maintenance.DatabaseInvalidatedEvent
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.core.storage.memoryCached
import com.gitlab.ykrasik.gamedex.core.util.flowScope
import com.gitlab.ykrasik.gamedex.util.*
import com.google.inject.BindingAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/06/2017
 * Time: 20:09
 */
@Singleton
class FileSystemServiceImpl @Inject constructor(
    @FileTreeStorage initialStorage: Storage<GameId, FileTree>,
    eventBus: EventBus,
) : FileSystemService {
    private val log = logger()

    private val storage = log.time("Reading file system cache...") {
        initialStorage.memoryCached()
    }

    init {
        flowScope(Dispatchers.IO) {
            eventBus.flowOf<GameEvent.Deleted>().forEach(debugName = "onGameDeleted") {
                it.games.forEach(::onGameDeleted)
            }
            eventBus.flowOf<DatabaseInvalidatedEvent>().forEach(debugName = "onDatabaseInvalidated") {
                onDbInvalidated()
            }
        }
    }

    override fun fileTree(gameId: GameId, path: File): StateFlow<FileTree?> {
        val fileTree = storage[gameId]
        val flow = MutableStateFlow(fileTree)

        // Refresh the cache, regardless of whether we got a hit or not - our cached result could already be invalid.
        // For some reason, this behaves much faster on the Default dispatcher instead of the IO dispatcher.
        GlobalScope.launch(Dispatchers.Default) {
            try {
                val newFileTree = calcFileTree(path)
                if (newFileTree != null && newFileTree != fileTree) {
                    storage[gameId] = newFileTree
                    flow.value = newFileTree
                }
            } catch (e: Exception) {
                log.error("Error reading $path", e)
            }
        }
        return flow
    }

    private fun calcFileTree(file: File): FileTree? {
        if (!file.exists()) return null

        return if (file.isDirectory) {
            val children = file.listFiles().asSequence().filter { !it.isHidden }.map { calcFileTree(it)!! }.toList()
            FileTree(
                name = file.name,
                size = children.fold(FileSize.Empty) { acc, f -> acc + f.size },
                isDirectory = true,
                children = children
            )
        } else {
            FileTree(
                name = file.name,
                size = FileSize(file.length()),
                isDirectory = false,
                children = emptyList()
            )
        }
    }

    override fun deleteCachedFileTree(gameId: GameId) {
        storage.delete(gameId)
    }

    override fun getFileTreeSizeTakenExcept(excludedGames: List<Game>): Map<GameId, FileSize> {
        val excudedKeys = excludedGames.mapTo(mutableSetOf()) { it.id }
        return storage.getAll().mapNotNullToMap { key, _ ->
            if (key in excudedKeys) return@mapNotNullToMap null
            key to FileSize(storage.sizeTaken(key))
        }
    }

    override suspend fun move(from: File, to: File) = withContext(Dispatchers.IO) {
        val canonicalFrom = from.canonicalFile
        val canonicalTo = to.canonicalFile

        val isRename = canonicalFrom.parentFile == canonicalTo.parentFile && from.name != to.name
        if (isRename) {
            doMove(from, to)
            return@withContext
        }

        val isSubFolderMove = canonicalTo.startsWith(canonicalFrom)
        if (isSubFolderMove) {
            val children = from.listFiles()!!
            to.mkdirs()
            children.forEach { file ->
                doMove(file, to.resolve(file.name))
            }
            return@withContext
        }

        to.parentFile.mkdirs()
        doMove(from, to)
    }

    private fun doMove(from: File, to: File) {
        // File.renameTo is case sensitive, but can fail (doesn't cover all move variants).
        // If it does, retry with Files.move, which is platform-independent (but also case insensitive)
        // and throws an exception if it fails.
        if (!from.renameTo(to)) {
            Files.move(from.toPath(), to.toPath())
        }
    }

    override suspend fun delete(file: File) = withContext(Dispatchers.IO) {
        file.deleteWithChildren()
    }

    override fun analyzeFolderName(rawName: String) = FileNameHandler.analyze(rawName)

    override fun sanitizeFileName(name: String) = FileNameHandler.sanitizeFileName(name)

    private fun onGameDeleted(game: Game) = deleteCachedFileTree(game.id)

    private fun onDbInvalidated() {
        log.debug("Invalidating file tree cache...")
        storage.clear()
    }

    // FIXME: Allow syncing cache to existing games, should be called on each game change by... someone.
}

@BindingAnnotation
@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FileTreeStorage