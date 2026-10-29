package tutorconnect.model.student;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Locale;

import org.junit.jupiter.api.Test;

public class WeeklySlotDomainTest {
    @Test
    public void everyDayAndMinute_roundTripsThroughTextAndStorageCodec() {
        for (DayOfWeek day : DayOfWeek.values()) {
            for (int minute = 0; minute < 1440; minute++) {
                LocalTime time = LocalTime.of(minute / 60, minute % 60);
                String text = String.format(Locale.ROOT, "%02d:%02d", time.getHour(), time.getMinute());
                WeeklySlot slot = new WeeklySlot(day, time);
                assertEquals(time, WeeklySlot.parseTime(text));
                assertEquals(text, slot.getDisplayTime());
                assertEquals(slot, WeeklySlotField.INSTANCE.decode(WeeklySlotField.INSTANCE.encode(slot)));
            }
        }
    }
}
