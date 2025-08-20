package by.lupach.oldtonew2.utils;

import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
public class DateParser {
    private DateParser(){}

    private final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter TS_TZ = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.US);

    public LocalDateTime parseOldSystemTimestamp(String value){
        if (value == null || value.isBlank()) return null;
        // try with zone (e.g., "2021-09-16 12:02:26 CDT")
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(value, TS_TZ);
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}
        // try without zone
        try {
            return LocalDateTime.parse(value, TS);
        } catch (DateTimeParseException e) {
            throw e;
        }
    }
}