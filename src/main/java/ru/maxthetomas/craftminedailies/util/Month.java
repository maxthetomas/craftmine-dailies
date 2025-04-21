package ru.maxthetomas.craftminedailies.util;

import net.minecraft.network.chat.Component;

import java.time.*;

public record Month(int year, int month) {
    public Month(int year, int month) {
        this.year = year;
        this.month = month;

        if (month < 0)
            throw new RuntimeException("Month cannot be <0");

        if (month > 11)
            throw new RuntimeException("Month cannot be >11");
    }

    public Month next() {
        var y = year;
        var m = month + 1;
        if (m > 11) {
            y++;
            m = 0;
        }

        return new Month(y, m);
    }

    public Month previous() {
        var y = year;
        var m = month - 1;
        if (m < 0) {
            y--;
            m = 11;
        }

        return new Month(y, m);
    }

    public String getMonthName() {
        return switch (this.month) {
            case 0 -> "January";
            case 1 -> "February";
            case 2 -> "March";
            case 3 -> "April";
            case 4 -> "May";
            case 5 -> "June";
            case 6 -> "July";
            case 7 -> "August";
            case 8 -> "September";
            case 9 -> "October";
            case 10 -> "November";
            case 11 -> "December";
            default -> "idk";
        };
    }

    public String getApiRepresentation() {
        return String.format("%s-%02d", year, month + 1);
    }

    public String getStringRepresentation() {
        return String.format("%s, %s", getMonthName(), year);
    }

    public Component getComponentRepresentation() {
        return Component.translatableWithFallback("craftminedailies.month", "%s, %s",
                Component.translatableWithFallback("craftminedailies.month." + getMonthName().toLowerCase(), getMonthName()),
                Component.literal(String.valueOf(year)));
    }

    public int startDay() {
        return 0;
    }

    /// Monday (0) -> Sunday (6)
    public int startDayOfWeek() {
        return YearMonth.of(this.year, this.month + 1)
                .atDay(1)
                .getDayOfWeek().getValue() - 1;
    }

    public int endDay() {
        var m = java.time.Month.of(this.month + 1);
        return m.length(Year.isLeap(year)) - 1;
    }

    public String getApiDay(int day) {
        return String.format("%s-%02d-%02d", year, month + 1, day + 1);
    }

    public static Day fromApiDay(String apiDay) {
        var parts = apiDay.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid API day format: " + apiDay);
        }

        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1; // Convert to 0-based month
        int day = Integer.parseInt(parts[2]) - 1;   // Convert to 0-based day

        return new Day(new Month(year, month), day);
    }

    public static Month current() {
        var now = Instant.now().atOffset(ZoneOffset.UTC);
        var year = now.getYear();
        var month = now.getMonth().getValue() - 1;

        return new Month(year, month);
    }

    public boolean isDayInFuture(int day) {
        var now = Instant.now().atOffset(ZoneOffset.UTC);
        var dayStart = YearMonth.of(this.year, this.month + 1)
                .atDay(day + 1).atStartOfDay();

        return now.isBefore(OffsetDateTime.of(dayStart, ZoneOffset.UTC));
    }

    public boolean isDayToday(int day) {
        var now = Instant.now().atOffset(ZoneOffset.UTC);
        var queriedDay = YearMonth.of(this.year, this.month + 1)
                .atDay(day + 1).atStartOfDay();

        return now.getDayOfMonth() == queriedDay.getDayOfMonth() &&
                now.getMonthValue() == queriedDay.getMonthValue() &&
                now.getYear() == queriedDay.getYear();
    }

    public boolean isDateBeforeModCreation(int day) {
        var dayStart = YearMonth.of(this.year, this.month + 1)
                .atDay(day + 1).atStartOfDay();

        return dayStart.isBefore(LocalDateTime.of(2025, 4, 17, 0, 0, 0));
    }

    public static boolean isMonthBeforeCreation(Month month) {
        if (month.year > 2025) return false;
        if (month.year <= 2024) return true;

        // 2025
        return month.month < 3;
    }

    public record Day(Month month, int day) {
        public String getStringRepresentation() {
            return String.format("%s %s", day + 1, month.getStringRepresentation());
        }

        public Component getComponentRepresentation() {
            return Component.literal(String.valueOf(day + 1)).append(" ").append(month.getComponentRepresentation());
        }
    }
}
