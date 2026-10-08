package seedu.address.model.person;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

import seedu.address.commons.util.StudentText;

/**
 * An immutable recurring weekly lesson. Day and minute-precision time can only be replaced together.
 */
public final class WeeklySlot implements Comparable<WeeklySlot> {
    public static final String MESSAGE_INVALID_DAY =
            "Error: Invalid day. Use Monday-Sunday or Mon-Sun.";
    public static final String MESSAGE_INVALID_TIME =
            "Error: Invalid time. Use 24-hour HH:mm, for example 09:00 or 18:30.";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private final DayOfWeek day;
    private final LocalTime time;

    /**
     * Creates a complete slot. Seconds and nanoseconds are not supported.
     */
    public WeeklySlot(DayOfWeek day, LocalTime time) {
        requireAllNonNull(day, time);
        if (time.getSecond() != 0 || time.getNano() != 0) {
            throw new IllegalArgumentException(MESSAGE_INVALID_TIME);
        }
        this.day = day;
        this.time = time;
    }

    /**
     * Parses full English weekday names and exactly their three-letter aliases, case-insensitively.
     */
    public static DayOfWeek parseDay(String input) {
        String value;
        try {
            value = StudentText.normalize(input);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MESSAGE_INVALID_DAY, e);
        }
        for (DayOfWeek candidate : DayOfWeek.values()) {
            String fullName = candidate.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            if (fullName.equalsIgnoreCase(value) || fullName.substring(0, 3).equalsIgnoreCase(value)) {
                return candidate;
            }
        }
        throw new IllegalArgumentException(MESSAGE_INVALID_DAY);
    }

    /**
     * Parses exactly HH:mm in the 24-hour clock, after shared normalization.
     */
    public static LocalTime parseTime(String input) {
        String value;
        try {
            value = StudentText.normalize(input);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(MESSAGE_INVALID_TIME, e);
        }
        if (!value.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
            throw new IllegalArgumentException(MESSAGE_INVALID_TIME);
        }
        return LocalTime.parse(value, TIME_FORMAT);
    }

    public DayOfWeek getDay() {
        return day;
    }

    public LocalTime getTime() {
        return time;
    }

    public String getDisplayDay() {
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public String getDisplayTime() {
        return time.format(TIME_FORMAT);
    }

    @Override
    public int compareTo(WeeklySlot other) {
        int dayOrder = day.compareTo(other.day);
        return dayOrder != 0 ? dayOrder : time.compareTo(other.time);
    }

    @Override
    public boolean equals(Object other) {
        return other == this || other instanceof WeeklySlot slot && day == slot.day && time.equals(slot.time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(day, time);
    }

    @Override
    public String toString() {
        return getDisplayDay() + " " + getDisplayTime();
    }
}
