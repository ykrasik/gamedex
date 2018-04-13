/****************************************************************************
 * Copyright (C) 2016-2018 Yevgeny Krasik                                   *
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

package com.gitlab.ykrasik.gamedex.core.persistence

import com.gitlab.ykrasik.gamedex.*
import com.gitlab.ykrasik.gamedex.util.fromJson
import com.gitlab.ykrasik.gamedex.util.listFromJson
import com.gitlab.ykrasik.gamedex.util.toFile
import com.gitlab.ykrasik.gamedex.util.toJsonStr
import com.google.inject.ImplementedBy
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
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
// TODO: This really feels like it needs to be broken up into small daos that the repositories will wrap.
@ImplementedBy(PersistenceServiceImpl::class)
interface PersistenceService {
    fun dropDb()

    fun fetchLibraries(): List<Library>
    fun insertLibrary(path: File, data: LibraryData): Library
    fun updateLibrary(library: Library): Boolean
    fun deleteLibrary(id: Int): Boolean
    fun deleteLibraries(libraryIds: List<Int>): Int

    fun fetchGames(): List<RawGame>
    fun insertGame(metadata: Metadata, providerData: List<ProviderData>, userData: UserData?): RawGame
    fun updateGame(rawGame: RawGame): Boolean
    fun deleteGame(id: Int): Boolean
    fun deleteGames(gameIds: List<Int>): Int
    fun clearUserData(): Int

    fun fetchImage(url: String): ByteArray?
    fun insertImage(gameId: Int, url: String, data: ByteArray)
    fun fetchImageSizesExcept(exceptUrls: List<String>): List<Pair<String, Long>>
    fun deleteImages(imageUrls: List<String>): Int
}

@Singleton
class PersistenceServiceImpl @Inject constructor(config: PersistenceConfig) : PersistenceService {
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

    override fun fetchLibraries() = transaction {
        Libraries.selectAll().map {
            Library(
                id = it[Libraries.id].value,
                path = it[Libraries.path].toFile(),
                data = it[Libraries.data].fromJson()
            )
        }
    }

    override fun insertLibrary(path: File, data: LibraryData) = transaction {
        val id = Libraries.insertAndGetId {
            it[Libraries.path] = path.toString()
            it[Libraries.data] = data.toJsonStr()
        }.value

        Library(id, path, data)
    }

    override fun updateLibrary(library: Library) = transaction {
        Libraries.update(where = { Libraries.id.eq(library.id.toLibraryId()) }) {
            it[path] = library.path.toString()
            it[data] = library.data.toJsonStr()
        } == 1
    }

    override fun deleteLibrary(id: Int) = transaction {
        Libraries.deleteWhere { Libraries.id.eq(id.toLibraryId()) } == 1
    }

    override fun deleteLibraries(libraryIds: List<Int>) = transaction {
        Libraries.deleteWhere { Libraries.id.inList(libraryIds) }
    }

    override fun fetchGames() = transaction {
        Games.selectAll().map {
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
    }

    override fun insertGame(metadata: Metadata, providerData: List<ProviderData>, userData: UserData?) = transaction {
        val id = Games.insertAndGetId {
            it[libraryId] = metadata.libraryId.toLibraryId()
            it[path] = metadata.path
            it[updateDate] = metadata.updateDate
            it[Games.providerData] = providerData.toJsonStr()
            it[Games.userData] = userData?.toJsonStr()
        }.value

        RawGame(id = id, metadata = metadata, providerData = providerData, userData = userData)
    }

    override fun updateGame(rawGame: RawGame) = transaction {
        Games.update(where = { Games.id.eq(rawGame.id.toGameId()) }) {
            it[libraryId] = rawGame.metadata.libraryId.toLibraryId()
            it[path] = rawGame.metadata.path
            it[updateDate] = rawGame.metadata.updateDate
            it[providerData] = rawGame.providerData.toJsonStr()
            it[userData] = rawGame.userData?.toJsonStr()
        } == 1
    }

    override fun deleteGame(id: Int) = transaction {
        Games.deleteWhere { Games.id.eq(id.toGameId()) } == 1
    }

    override fun deleteGames(gameIds: List<Int>) = transaction {
        Games.deleteWhere { Games.id.inList(gameIds) }
    }

    override fun clearUserData() = transaction {
        Games.updateAll {
            it[userData] = null
        }
    }

    override fun fetchImage(url: String) = transaction {
        Images.select { Images.url.eq(url) }.map {
            it[Images.bytes].bytes
        }.firstOrNull()
    }

    override fun insertImage(gameId: Int, url: String, data: ByteArray): Unit = transaction {
        Images.insert {
            it[Images.gameId] = gameId.toGameId()
            it[Images.url] = url
            it[bytes] = data.toBlob()
        }
    }

    override fun fetchImageSizesExcept(exceptUrls: List<String>): List<Pair<String, Long>> = transaction {
        Images.select { Images.url.notInList(exceptUrls) }.map {
            it[Images.url] to it[Images.bytes].length()
        }
    }

    override fun deleteImages(imageUrls: List<String>) = transaction {
        Images.deleteWhere { Images.url.inList(imageUrls) }
    }

    private fun Int.toLibraryId() = EntityID(this, Libraries)
    private fun Int.toGameId() = EntityID(this, Games)

    private val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())
    private fun ByteArray.toBlob(): Blob = SerialBlob(this)
}