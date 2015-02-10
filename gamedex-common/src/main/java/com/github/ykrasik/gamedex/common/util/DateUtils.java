package com.github.ykrasik.gamedex.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author Yevgeny Krasik
 */
public final class DateUtils {
    private DateUtils() {
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return toLocalDateTime(date, ZoneId.systemDefault());
    }

    public static LocalDateTime toLocalDateTime(Date date, ZoneId timeZoneId) {
        return date.toInstant().atZone(timeZoneId).toLocalDateTime();
    }

    public static Date fromLocalDateTime(LocalDateTime date) {
        return fromLocalDateTime(date, ZoneId.systemDefault());
    }

    public static Date fromLocalDateTime(LocalDateTime date, ZoneId timeZoneId) {
        return Date.from(date.atZone(timeZoneId).toInstant());
    }

    public static LocalDate toLocalDate(Date date) {
        return toLocalDate(date, ZoneId.systemDefault());
    }

    public static LocalDate toLocalDate(Date date, ZoneId timeZoneId) {
        return toLocalDateTime(date, timeZoneId).toLocalDate();
    }

    public static Date fromLocalDate(LocalDate date) {
        return fromLocalDate(date, ZoneId.systemDefault());
    }

    public static Date fromLocalDate(LocalDate date, ZoneId timeZoneId) {
        return fromLocalDateTime(date.atStartOfDay(), timeZoneId);
    }
}
