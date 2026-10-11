package tutorconnect.model;

import java.util.UUID;
import java.util.function.Predicate;

import javafx.collections.ObservableList;
import tutorconnect.commons.core.GuiSettings;
import tutorconnect.model.student.Student;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true */
    Predicate<Student> PREDICATE_SHOW_ALL_STUDENTS = unused -> true;

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces address book data with the data in {@code studentDirectory}.
     */
    void setStudentDirectory(ReadOnlyStudentDirectory studentDirectory);

    /** Returns the StudentDirectory */
    ReadOnlyStudentDirectory getStudentDirectory();

    /**
     * Returns true if a student with the same identity as {@code student} exists in the address book.
     */
    boolean hasStudent(Student student);

    /**
     * Deletes the given student.
     * The student must exist in the address book.
     */
    void deleteStudent(Student target);

    /**
     * Adds the given student.
     * {@code student} must not already exist in the address book.
     */
    void addStudent(Student student);

    /**
     * Replaces the given student {@code target} with {@code editedStudent}.
     * {@code target} must exist in the address book.
     * The student identity of {@code editedStudent} must not be the same as another existing student in the address
     * book.
     */
    void setStudent(Student target, Student editedStudent);

    /** Returns an unmodifiable view of the filtered student list */
    ObservableList<Student> getFilteredStudentList();

    /**
     * Updates the filter of the filtered student list to filter by the given {@code predicate}.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredStudentList(Predicate<Student> predicate);

    /** Snapshot used by LogicManager. Commands must never write storage directly. */
    default Model copyForCommand() {
        ModelManager copy = new ModelManager(getStudentDirectory(), getUserPrefs());
        copy.updateFilteredStudentList(getStudentPredicate());
        copy.selectStudent(getSelectedStudentId());
        return copy;
    }

    /** Publishes a complete, already-saved state. */
    default void publish(Model proposed) {
        if (!getStudentDirectory().equals(proposed.getStudentDirectory())) {
            setStudentDirectory(proposed.getStudentDirectory());
        }
        updateFilteredStudentList(proposed.getStudentPredicate());
        selectStudent(proposed.getSelectedStudentId());
    }

    default Predicate<Student> getStudentPredicate() {
        return PREDICATE_SHOW_ALL_STUDENTS;
    }

    /** Null denotes no selection. F02/F03 resolves displayed indices to these stable IDs. */
    default UUID getSelectedStudentId() {
        return null;
    }

    /** Optional hook for lightweight test doubles; ModelManager implements selection. */
    default void selectStudent(UUID id) {}
}
