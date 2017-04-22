package com.gitlab.ykrasik.gamedex.util

/**
 * User: ykrasik
 * Date: 02/04/2017
 * Time: 14:44
 */
data class ProgramData(
    val amountOfDiComponents: Int = 17
) {

    companion object {
        private val file = "conf/data.json".toFile()

        fun get(): ProgramData = file.existsOrNull()?.readJson() ?: ProgramData()
        fun write(data: ProgramData) {
            if (!file.exists()) {
                file.create()
            }
            file.writeJson(data)
        }
    }
}