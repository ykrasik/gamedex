package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.util.toHumanReadableFileSize
import javafx.beans.property.ReadOnlyStringProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import java.io.File
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/06/2017
 * Time: 20:09
 */
@Singleton
open class FileSystemOps {
    // TODO: not the cleanest solution, whatever
    private val sizeCache = mutableMapOf<File, StringProperty>()

    fun size(file: File): ReadOnlyStringProperty = sizeCache.getOrElse(file) {
        val sizeProperty = SimpleStringProperty()
        launch(CommonPool) {
            val size = file.walkBottomUp()
                .fold(0L) { acc, f -> if (f.isFile) acc + f.length() else acc }
                .toHumanReadableFileSize()
            run(JavaFx) {
                sizeProperty.value = size
            }
            sizeCache[file] = sizeProperty
        }
        return sizeProperty
    }
}