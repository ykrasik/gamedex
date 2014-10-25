package com.github.ykrasik.indexter.games.data.translator;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author Yevgeny Krasik
 */
public abstract class AbstractEntityTranslator {
    protected LocalDate translateDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    protected Date translateDate(LocalDate date) {
        return Date.from(date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }
}
