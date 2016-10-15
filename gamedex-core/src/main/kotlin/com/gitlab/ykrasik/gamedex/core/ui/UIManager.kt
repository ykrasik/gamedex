package com.gitlab.ykrasik.gamedex.core.ui

import com.github.ykrasik.gamedex.datamodel.ExcludedPath
import com.github.ykrasik.gamedex.datamodel.Game
import com.github.ykrasik.gamedex.datamodel.HasPath
import com.github.ykrasik.gamedex.datamodel.Library
import com.github.ykrasik.gamedex.datamodel.provider.GameData
import java.nio.file.Path

/**
 * User: ykrasik
 * Date: 15/10/2016
 * Time: 13:57
 */
interface UIManager<T : HasPath> {
    val all: List<T>
    operator fun get(path: Path): T? = all.find { it.path == path }
    operator fun contains(path: Path): Boolean = get(path) != null
}

interface GameUIManager : UIManager<Game> {
    fun add(gameData: GameData, path: Path, library: Library): Game
}

interface LibraryUIManager : UIManager<Library>

interface ExcludedPathUIManager : UIManager<ExcludedPath>