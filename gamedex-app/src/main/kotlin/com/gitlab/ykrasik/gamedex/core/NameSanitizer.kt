package com.gitlab.ykrasik.gamedex.core

import javax.inject.Singleton

/**
 * User: ykrasik
 * Date: 11/06/2017
 * Time: 13:54
 */
@Singleton
class NameSanitizer {
    private val metaDataRegex = "(\\[.*?\\])".toRegex()
    private val spacesRegex = "\\s+".toRegex()

    // Remove all metaData enclosed with '[]' from the file name and collapse all spaces into a single space.
    fun sanitize(unsanitized: String) = unsanitized.replace(metaDataRegex, "").collapseSpaces().sanitizeDash().trim()

    fun removeMetaSymbols(unsanitized: String) =
        unsanitized.replace("[", "").replace("]", "").collapseSpaces().trim()

    fun toValidFileName(unsanitized: String) =
        unsanitized.replace(": ", " - ").replace("/", " ").replace("\\", " ").collapseSpaces().trim()

    private fun String.sanitizeDash() = replace(" - ", ": ")
    private fun String.collapseSpaces() = spacesRegex.replace(this, " ")
}