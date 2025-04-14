package ru.maxthetomas.craftminedailies.util;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class TimeFormatters {
    public static long secondsUntilNextDaily() {
        // Get the current time in UTC
        ZonedDateTime nowUTC = ZonedDateTime.now(ZoneOffset.UTC);

        // Get the time of the start of the *next* day in UTC
        ZonedDateTime nextDayUTC = nowUTC.toLocalDate()
                .plusDays(1)
                .atStartOfDay(ZoneOffset.UTC);

        // Calculate the duration between now and the next day's start
        Duration duration = Duration.between(nowUTC, nextDayUTC);

        // Get total seconds (ensures accuracy across day boundaries)
        return duration.getSeconds();
    }

    public static String formatTime(long totalSeconds) {
        // Calculate hours, minutes, and remaining seconds
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatTimeWithoutHours(long totalSeconds) {
        // Calculate hours, minutes, and remaining seconds
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }
}
