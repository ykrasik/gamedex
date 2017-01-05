package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.ImageData
import com.gitlab.ykrasik.gamedex.persistence.entity.Images
import com.gitlab.ykrasik.gamedex.persistence.entity.bytes
import com.gitlab.ykrasik.gamedex.persistence.entity.toBlob
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.sql.Blob

/**
 * User: ykrasik
 * Date: 02/01/2017
 * Time: 18:30
 */
interface ImageDao {
    fun fetchThumbnail(gameId: Int): ImageData
    fun updateThumbnail(gameId: Int, data: ImageData): Unit
}

class ImageDaoImpl : ImageDao {
    private val log by logger()

    override fun fetchThumbnail(gameId: Int): ImageData {
        log.debug { "Fetching thumbnail: gameId=$gameId..." }
        val data = fetchImage(gameId, dataColumn = Images.thumbnail, urlColumn = Images.thumbnailUrl)
        log.debug { "Result: $data" }
        return data
    }

    override fun updateThumbnail(gameId: Int, data: ImageData) {
        log.debug { "Updating thumbnail: gameId=$gameId..." }
        updateImage(gameId, data, dataColumn = Images.thumbnail, urlColumn = Images.thumbnailUrl)
        log.debug { "Result: $data" }
    }

    private fun fetchImage(gameId: Int, dataColumn: Column<Blob?>, urlColumn: Column<String?>): ImageData = transaction {
        val thumbnail = Images.slice(dataColumn, urlColumn).select { Images.gameId.eq(gameId) }.firstOrNull()!!
        thumbnail.toImageData(dataColumn, urlColumn)
    }

    private fun ResultRow.toImageData(dataColumn: Column<Blob?>, urlColumn: Column<String?>): ImageData = ImageData(
        bytes = this[dataColumn]?.bytes,
        url = this[urlColumn]
    )

    private fun updateImage(gameId: Int, data: ImageData, dataColumn: Column<Blob?>, urlColumn: Column<String?>): Unit = transaction {
        val count = Images.update(where = { Images.gameId.eq(gameId) }) {
            it[dataColumn] = data.bytes?.toBlob()
            it[urlColumn] = data.url
        }
        check(count == 1) { "Updated invalid amount of rows ($count) when updating image for gameId=$gameId!" }
    }
}