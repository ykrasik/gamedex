/****************************************************************************
 * Copyright (C) 2016-2019 Yevgeny Krasik                                   *
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
import com.gitlab.ykrasik.gamedex.core.game.GamesDeletedEvent
import com.gitlab.ykrasik.gamedex.core.maintenance.DatabaseInvalidatedEvent
import com.gitlab.ykrasik.gamedex.core.on
import com.gitlab.ykrasik.gamedex.core.storage.Storage
import com.gitlab.ykrasik.gamedex.core.storage.memoryCached
import com.gitlab.ykrasik.gamedex.util.*
import com.google.inject.BindingAnnotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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
    eventBus: EventBus
) : FileSystemService {
    private val log = logger()

    private val storage = log.time("Reading file system cache...") {
        initialStorage.memoryCached()
    }

    init {
        eventBus.on<GamesDeletedEvent>(Dispatchers.IO) { it.games.forEach(::onGameDeleted) }
        eventBus.on<DatabaseInvalidatedEvent>(Dispatchers.IO) { onDbInvalidated() }
    }

    override fun fileTree(gameId: GameId, path: File): Ref<FileTree?> {
        val fileTree = storage[gameId]
        val ref = ref(fileTree)

        // Refresh the cache, regardless of whether we got a hit or not - our cached result could already be invalid.
        GlobalScope.launch(Dispatchers.IO) {
            val newFileTree = calcFileTree(path)
            if (newFileTree != null && newFileTree != fileTree) {
                storage[gameId] = newFileTree
                ref.value = newFileTree
            }
        }
        return ref
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
        to.parentFile.mkdirs()

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

    override fun toFileName(name: String) = FileNameHandler.toFileName(name)

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