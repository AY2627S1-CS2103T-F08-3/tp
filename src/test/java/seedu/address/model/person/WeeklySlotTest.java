package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class WeeklySlotTest {
    @Test
    public void parseDay_allSevenFullNamesAndAliases_caseInsensitive() {
        for (DayOfWeek day : DayOfWeek.values()) {
            String full = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String alias = full.substring(0, 3);
            for (String value : List.of(full, alias, full.toLowerCase(Locale.ROOT),
                    full.toUpperCase(Locale.ROOT), alias.toLowerCase(Locale.ROOT), alias.toUpperCase(Locale.ROOT))) {
                assertEquals(day, WeeklySlot.parseDay(value));
                assertEquals(full, new WeeklySlot(day, LocalTime.NOON).getDisplayDay());
            }
        }
    }

    @Test
    public void parseDay_sharedNormalization() {
        assertEquals(DayOfWeek.TUESDAY, WeeklySlot.parseDay("  Ｔｕｅ\u00a0 "));
        assertEquals(DayOfWeek.SUNDAY, WeeklySlot.parseDay("\u3000sUnDaY  "));
    }

    @Test
    public void parseDay_invalidNamesAndControls_exactMessage() {
        for (String value : List.of("", " ", "Tues", "Thurs", "M", "Mo", "Weekday", "Funday",
                "Tuesday Wednesday", "Tuesday\n", "\tTue", "Tue\u0000", "Tue\u2028", "Tue\u200b")) {
            assertThrows(IllegalArgumentException.class, WeeklySlot.MESSAGE_INVALID_DAY,
                    () -> WeeklySlot.parseDay(value));
        }
    }

    @Test
    public void parseTime_validBoundaries_andDisplayKeepsTwoDigits() {
        for (String value : List.of("00:00", "00:01", "09:00", "09:30", "18:30", "23:00", "23:59")) {
            LocalTime time = WeeklySlot.parseTime(value);
            assertEquals(LocalTime.parse(value), time);
            assertEquals(value, new WeeklySlot(DayOfWeek.SATURDAY, time).getDisplayTime());
        }
        assertEquals(LocalTime.of(9, 0), WeeklySlot.parseTime("  ０９：００\u00a0"));
    }

    @Test
    public void parseTime_invalidFormatsRangesAndControls_exactMessage() {
        for (String value : List.of("", " ", "9:00", "09:0", "6pm", "18.30", "24:00", "12:60",
                "-1:00", "+9:00", "009:00", "09:000", "09:00:00", "09 :00", "09: 00",
                "09:00Z", "９:００", "09:00\n", "\t09:00", "09:00\u0000", "09:00\u2029")) {
            assertThrows(IllegalArgumentException.class, WeeklySlot.MESSAGE_INVALID_TIME,
                    () -> WeeklySlot.parseTime(value));
        }
    }

    @Test
    public void constructor_requiresBothComponentsAndMinutePrecision() {
        assertThrows(NullPointerException.class, () -> new WeeklySlot(null, LocalTime.NOON));
        assertThrows(NullPointerException.class, () -> new WeeklySlot(DayOfWeek.MONDAY, null));
        assertThrows(IllegalArgumentException.class, WeeklySlot.MESSAGE_INVALID_TIME,
                () -> new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(9, 0, 1)));
        assertThrows(IllegalArgumentException.class, WeeklySlot.MESSAGE_INVALID_TIME,
                () -> new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(9, 0, 0, 1)));
    }

    @Test
    public void equalityHashAndOrdering_useWeekdayThenTime() {
        WeeklySlot monday = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(23, 59));
        WeeklySlot tuesday = new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.MIDNIGHT);
        WeeklySlot laterTuesday = new WeeklySlot(DayOfWeek.TUESDAY, LocalTime.of(0, 1));
        WeeklySlot sameMonday = new WeeklySlot(DayOfWeek.MONDAY, LocalTime.of(23, 59));
        assertEquals(monday, sameMonday);
        assertEquals(monday.hashCode(), sameMonday.hashCode());
        assertEquals(0, monday.compareTo(sameMonday));
        assertNotEquals(monday, tuesday);
        assertNotEquals(monday, null);
        assertNotEquals(monday, "Monday 23:59");
        assertTrue(monday.compareTo(tuesday) < 0);
        assertTrue(tuesday.compareTo(laterTuesday) < 0);
        assertEquals("Monday 23:59", monday.toString());
    }
}
