package seedu.address.model;

import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import seedu.address.model.person.Person;
import seedu.address.model.person.WeeklySlotField;

/**
 * Observes the register and exposes an independent weekly ordering; register positions never change.
 */
public final class WeeklySchedule {
    private final ObservableList<Person> entries;

    /**
     * Orders scheduled students by Monday-Sunday, time, then source insertion order.
     */
    public WeeklySchedule(ObservableList<Person> register) {
        FilteredList<Person> scheduled = new FilteredList<>(register,
                person -> WeeklySlotField.get(person).isPresent());
        SortedList<Person> ordered = new SortedList<>(scheduled,
                Comparator.comparing((Person person) -> WeeklySlotField.get(person).orElseThrow())
                        .thenComparingInt(register::indexOf));
        entries = FXCollections.unmodifiableObservableList(ordered);
    }

    public ObservableList<Person> getEntries() {
        return entries;
    }
}
