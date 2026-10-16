package tutorconnect.model;

import java.util.Comparator;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import tutorconnect.model.student.Student;
import tutorconnect.model.student.WeeklySlotField;

/**
 * Observes the register and exposes an independent weekly ordering; register positions never change.
 */
public final class WeeklySchedule {
    private final ObservableList<Student> entries;

    /**
     * Orders scheduled students by Monday-Sunday, time, then source insertion order.
     */
    public WeeklySchedule(ObservableList<Student> register) {
        FilteredList<Student> scheduled = new FilteredList<>(register,
                student -> WeeklySlotField.get(student).isPresent());
        SortedList<Student> ordered = new SortedList<>(scheduled,
                Comparator.comparing((Student student) -> WeeklySlotField.get(student).orElseThrow())
                        .thenComparingInt(register::indexOf));
        entries = FXCollections.unmodifiableObservableList(ordered);
    }

    public ObservableList<Student> getEntries() {
        return entries;
    }
}
