package com.gitlab.ykrasik.gamedex.persistence.dao

import com.github.ykrasik.gamedex.common.logger
import com.github.ykrasik.gamedex.datamodel.GameImage
import com.github.ykrasik.gamedex.datamodel.GameImageId
import com.github.ykrasik.gamedex.datamodel.GameImageType
import com.gitlab.ykrasik.gamedex.persistence.entity.Images
import com.gitlab.ykrasik.gamedex.persistence.entity.bytes
import com.gitlab.ykrasik.gamedex.persistence.entity.toBlob
import org.jetbrains.exposed.sql.Column
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
        GameImage(
            id = id,
            bytes = result[dataColumn]?.bytes,
            url = result[urlColumn]
        )
    }

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