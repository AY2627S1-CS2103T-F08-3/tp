package tutorconnect.logic.commands;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.Optional;

/** A read-only UI request; an empty day means the complete week. Never persisted as student data. */
public record TimetableView(Optional<DayOfWeek> day) {
    public TimetableView {
        Objects.requireNonNull(day);
    }
}
