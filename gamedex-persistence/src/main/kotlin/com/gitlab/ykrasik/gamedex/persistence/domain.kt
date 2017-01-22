package com.gitlab.ykrasik.gamedex.persistence

import com.github.ykrasik.gamedex.datamodel.GameData
import com.github.ykrasik.gamedex.datamodel.GameImageData
import com.github.ykrasik.gamedex.datamodel.LibraryData
import java.io.File

/**
 * User: ykrasik
 * Date: 22/01/2017
 * Time: 09:09
 */

data class AddLibraryRequest(
    val path: File,
    val data: LibraryData
)

data class AddGameRequest(
    val libraryId: Int,
    val path: File,
    val gameData: GameData,
    val imageData: GameImageData
)