package com.gitlab.ykrasik.gamedex.core

import com.gitlab.ykrasik.gamedex.util.FileSize
import com.gitlab.ykrasik.gamedex.util.sizeTaken
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyObjectProperty
import javafx.beans.property.SimpleObjectProperty
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.run
import tornadofx.toProperty
import java.io.File
import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 30/06/2017
 * Time: 20:09
 */
@Singleton
class FileSystemOps {
    private val sizeCache = mutableMapOf<File, ObjectProperty<FileSize>>()

    fun size(file: File): ReadOnlyObjectProperty<FileSize> {
        val existing = sizeCache[file]
        if (existing?.value?.bytes ?: 0L != 0L) return existing!!

        return SimpleObjectProperty<FileSize>(FileSize(0L)).let { sizeProperty ->
            sizeCache[file] = sizeProperty
            launch(CommonPool) {
                val size = file.sizeTaken()
                run(JavaFx) {
                    sizeProperty.value = size
                }
            }
            sizeProperty
        }
    }

    fun sizeSync(file: File): FileSize {
        sizeCache[file]?.let { if (it.value.bytes > 0) return it.value }

        val size = file.sizeTaken()
        sizeCache[file] = size.toProperty()
        return size
    }
}