package com.choicecrafter.studentapp.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

public class TimeAgoUtil {

    private static final DateTimeFormatter FLEX_PARSER =
            new DateTimeFormatterBuilder()
                    .appendPattern("uuuu-MM-dd'T'HH:mm:ss")
                    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                    .optionalStart().appendOffsetId().optionalEnd()
                    .toFormatter();
    private static final DateTimeFormatter ISO_MILLIS_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getTimeAgo(String timestamp) {
        Instant timestampInstant = parseInstant(timestamp, ZoneId.systemDefault());
        return formatDurationFrom(timestampInstant);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getTimeAgo(long timestampMillis) {
        Instant timestampInstant = Instant.ofEpochMilli(timestampMillis);
        return formatDurationFrom(timestampInstant);
    }

    public static String toIsoMillis(String dateTime, ZoneId zone) {
        Instant instant = parseInstant(dateTime, zone);
        long epochMillis = instant.toEpochMilli();
        return ISO_MILLIS_FORMATTER
                .withZone(zone)
                .format(Instant.ofEpochMilli(epochMillis));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Instant parseInstant(String timestamp, ZoneId zone) {
        TemporalAccessor ta = FLEX_PARSER.parse(timestamp);
        return ta.isSupported(ChronoField.OFFSET_SECONDS)
                ? OffsetDateTime.from(ta).toInstant()
                : LocalDateTime.from(ta).atZone(zone).toInstant();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String formatDurationFrom(Instant timestampInstant) {
        Duration duration = Duration.between(timestampInstant, Instant.now());

        if (duration.toMinutes() < 1) {
            return "Just now";
        } else if (duration.toHours() < 1) {
            return duration.toMinutes() + "m ago";
        } else if (duration.toDays() < 1) {
            return duration.toHours() + "h ago";
        } else if (duration.toDays() < 7) {
            return duration.toDays() + " days ago";
        } else if (duration.toDays() < 30) {
            return (duration.toDays() / 7) + " weeks ago";
        } else if (duration.toDays() < 365) {
            return (duration.toDays() / 30) + " months ago";
        } else {
            return (duration.toDays() / 365) + " years ago";
        }
    }
}
