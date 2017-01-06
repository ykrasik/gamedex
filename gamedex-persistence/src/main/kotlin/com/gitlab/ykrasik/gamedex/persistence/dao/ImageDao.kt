package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GameImage
import com.github.ykrasik.gamedex.datamodel.GameImageId
import com.github.ykrasik.gamedex.datamodel.GameImageType
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
    fun fetchImage(id: GameImageId): GameImage
    fun updateImage(image: GameImage): Unit
}

class ImageDaoImpl : ImageDao {
    private val log by logger()

    override fun fetchImage(id: GameImageId): GameImage {
        log.debug { "Fetching: $id..." }
        val (dataColumn, urlColumn) = id.toColumns()
        val data = fetchImage(id, dataColumn, urlColumn)
        log.debug { "Result: $data." }
        return data
    }

    private fun fetchImage(id: GameImageId, dataColumn: Column<Blob?>, urlColumn: Column<String?>): GameImage = transaction {
        val result = Images.slice(dataColumn, urlColumn).select { Images.gameId.eq(id.gameId) }.first()
        result.toGameImage(id, dataColumn, urlColumn)
    }

    private fun ResultRow.toGameImage(id: GameImageId,
                                      dataColumn: Column<Blob?>,
                                      urlColumn: Column<String?>) = GameImage(
        id = id,
        bytes = this[dataColumn]?.bytes,
        url = this[urlColumn]
    )

    override fun updateImage(image: GameImage) {
        log.debug { "Updating: image..." }
        val (dataColumn, urlColumn) = image.id.toColumns()
        updateImage(image, dataColumn, urlColumn)
        log.debug { "Done." }
    }

    private fun updateImage(image: GameImage, dataColumn: Column<Blob?>, urlColumn: Column<String?>): Unit = transaction {
        val count = Images.update(where = { Images.gameId.eq(image.id.gameId) }) {
            it[dataColumn] = image.bytes?.toBlob()
            it[urlColumn] = image.url
        }
        check(count == 1) { "Updated invalid amount of rows ($count) when updating image for $image!" }
    }

    private fun GameImageId.toColumns(): Pair<Column<Blob?>, Column<String?>> = when(this.type) {
        GameImageType.Thumbnail -> Pair(Images.thumbnail, Images.thumbnailUrl)
        GameImageType.Poster -> Pair(Images.poster, Images.posterUrl)
        else -> throw IllegalArgumentException("Invalid gameImageType: ${this.type}!")
    }
}