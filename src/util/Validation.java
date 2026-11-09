package util;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Validation {

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static double money(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    public static LocalDate parseDate(String value) {
        return LocalDate.parse(value);
    }

    public static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value);
    }

    public static int validateJourney(int from, int to, int stopCount) {

        check(from >= 0 && to < stopCount && from < to,
                "Invalid boarding or dropping point.");

        return from;
    }
}