package com.gitlab.ykrasik.gamedex.provider.giantbomb.jackson

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.joda.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * User: ykrasik
 * Date: 02/10/2016
 * Time: 09:31
 */
// TODO: Might be possible to use JsonFormat instead, but it's a bit tricky.
class GiantBombJacksonDateDeserializer : JsonDeserializer<LocalDate?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDate? {
        // The date comes at a non-standard format, with a ' ' between the date and time (rather then 'T' as ISO dictates).
        // We don't care about the time anyway, just parse the date.
        return try {
            val raw = p.valueAsString
            val indexOfSpace = raw.indexOf(' ')
            val toParse = if (indexOfSpace != -1) raw.substring(0, indexOfSpace) else raw
            LocalDate.parse(toParse)
        } catch (ignored: DateTimeParseException) {
            null
        }
    }
}