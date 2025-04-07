package com.accountservice.utils;

import java.time.*;
import java.util.Date;

public class DateUtils {
    public static Date convertLocalDateTimeToDate(LocalDateTime localDateTime) {
        Instant instant = localDateTime.toInstant(ZoneOffset.UTC);
        return Date.from(instant);
    }
    public static Date getDate(int daysToSubtract) {
        return convertLocalDateTimeToDate(LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusDays(daysToSubtract));
    }
}
