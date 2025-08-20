package by.lupach.oldtonew.utils;

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
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(value, TS_TZ);
            return zdt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {}
        return LocalDateTime.parse(value, TS);
    }
}