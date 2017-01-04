package com.gitlab.ykrasik.gamedex.persistence.entity

import java.sql.Blob
import javax.sql.rowset.serial.SerialBlob

/**
 * User: ykrasik
 * Date: 25/05/2016
 * Time: 09:12
 */

// TODO: don't use this, stream the blob & report progress.
val Blob.bytes: ByteArray get() = getBytes(0, length().toInt())

fun ByteArray.toBlob(): Blob = SerialBlob(this)